/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */

package org.knime.python2.kernel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

import com.google.common.primitives.Ints;

/**
 * Used for communicating with the python kernel via commands sent over sockets.
 *
 * @author Clemens von Schwerin, KNIME GmbH, Konstanz, Germany
 */
public class Commands {

    private final OutputStream m_outToServer;

    private final InputStream m_inFromServer;

    private final DataInputStream m_bufferedInFromServer;

    private final DataOutputStream m_bufferedOutToServer;

    private final CommandsMessages m_messages;

    private final Lock m_lock;

    private int m_msgIdCtr;

    private Executor m_executor;

    private NodeLogger LOGGER = NodeLogger.getLogger(Commands.class);

    private AtomicBoolean m_msgLoopRunning;

    private CommandsHandler m_commandsHandler;

    /**
     * Constructor.
     *
     * @param outToServer output stream of the socket used for communication with the python kernel
     * @param inFromServer input stream of the socket used for communication with the python kernel
     */
    public Commands(final OutputStream outToServer, final InputStream inFromServer) {
        m_lock = new ReentrantLock();
        m_outToServer = outToServer;
        m_inFromServer = inFromServer;
        m_bufferedInFromServer = new DataInputStream(m_inFromServer);
        m_bufferedOutToServer = new DataOutputStream(m_outToServer);
        m_messages = new CommandsMessages(this);
        m_msgIdCtr = 0;
        //TODO KNIME Threadpool ?
        m_executor = Executors.newSingleThreadExecutor();
        m_msgLoopRunning = new AtomicBoolean(true);
        m_commandsHandler = new CommandsHandler();
        m_messages.registerMessageHandler(m_commandsHandler);
        m_executor.execute(new Runnable() {

            @Override
            public void run() {
                while(m_msgLoopRunning.get()) {
                    try {
                        CommandMessage msg = m_messages.readMessage();
                        m_messages.handleMessage(msg);
                    } catch (IOException ex) {
                        if(m_msgLoopRunning.get()) {
                            LOGGER.warn("Could not read messge, cause: " + ex.getMessage());
                        }
                    }
                }

            }});
    }

    public void stopMessageLoop() {
        m_msgLoopRunning.set(false);
    }

    /**
     * @return returns the command's messaging interface
     */
    public Messages getMessages() {
        return m_messages;
    }

    public void sendMessage(final CommandMessage msg) {
        byte[] header = msg.getHeader().getBytes(StandardCharsets.UTF_8);
        byte[] payload = msg.getPayload();
        try {
            m_bufferedOutToServer.writeInt(header.length);
            m_bufferedOutToServer.writeInt(payload.length);
            m_bufferedOutToServer.write(header);
            m_bufferedOutToServer.write(payload);
        } catch (IOException ex) {
            LOGGER.error("IOException when sending message, cause: " + ex.getMessage());
        }

    }



    /**
     * Get the python kernel's process id.
     *
     * @return the process id
     * @throws IOException
     */
    public Future<Integer> getPid() throws IOException {
        m_lock.lock();
        try {
            final int id = m_msgIdCtr++;
            CommandMessage msg = new CommandMessage(id, "getpid", null, true, Optional.empty());
            Future<Integer> result = m_commandsHandler.registerResponse(msg, new Function<CommandMessage, Integer>(){

                @Override
                public Integer apply(final CommandMessage t) {
                    return Ints.fromByteArray(t.getPayload());
                }});
            sendMessage(msg);
            return result;
        } finally {
            m_lock.unlock();
        }

    }

    /**
     * Execute a source code snippet in the python kernel.
     *
     * @param sourceCode the snippet to execute
     * @return warning or error messages that were emitted during execution
     * @throws IOException
     */
    public Future<String[]> execute(final String sourceCode) throws IOException {
        m_lock.lock();
        try {
            final int id = m_msgIdCtr++;
            CommandMessage msg = new CommandMessage(id, "execute", sourceCode.getBytes(StandardCharsets.UTF_8), true, Optional.empty());
            Future<String[]> result = m_commandsHandler.registerResponse(msg, new Function<CommandMessage, String[]>(){

                @Override
                public String[] apply(final CommandMessage t) {
                    String outAndErrStr = new String(t.getPayload(), StandardCharsets.UTF_8);
                    String[] outAndErr = outAndErrStr.split(";");
                    return outAndErr;
                }});
            /*sendMessage(msg);
            return result;
            writeString("execute");
            writeString(sourceCode);
            m_messages.waitForSuccessMessage();
            final String[] output = new String[2];
            output[0] = readString();
            output[1] = readString(); */
            return result;
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Put some serialized flow variables into the python workspace. The flow variables should be serialized using the
     * currently active serialization library.
     *
     * @param name the name of the variable in the python workspace
     * @param variables the serialized variables table as bytearray
     * @throws IOException
     */
    public void putFlowVariables(final String name, final byte[] variables) throws IOException {
        m_lock.lock();
        try {
            writeString("putFlowVariables");
            writeString(name);
            writeBytes(variables);
            readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Get some serialized flow variables from the python workspace.
     *
     * @param name the variable name in the python workspace
     * @return the serialized variables table as bytearray
     * @throws IOException
     */
    public byte[] getFlowVariables(final String name) throws IOException {
        m_lock.lock();
        try {
            writeString("getFlowVariables");
            writeString(name);
            return readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Put a serialized KNIME table into the python workspace (as pandas.DataFrame). The table should be serialized
     * using the currently active serialization library.
     *
     * @param name the name of the variable in python workspace
     * @param table the serialized KNIME table as bytearray
     * @throws IOException
     */
    public void putTable(final String name, final byte[] table) throws IOException {
        m_lock.lock();
        try {
            writeString("putTable");
            writeString(name);
            writeBytes(table);
            m_messages.waitForSuccessMessage();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Append a chunk of table rows to a table represented as pandas.DataFrame in the python workspace. The table chunk
     * should be serialized using the currently active serialization library.
     *
     * @param name the name of the variable in the python workspace
     * @param table the serialized table chunk as bytearray
     * @throws IOException
     */
    public void appendToTable(final String name, final byte[] table) throws IOException {
        m_lock.lock();
        try {
            writeString("appendToTable");
            writeString(name);
            writeBytes(table);
            m_messages.waitForSuccessMessage();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Get the size in bytes of a serialized table from the python workspace.
     *
     * @param name the variable name
     * @return the size in bytes
     * @throws IOException
     */
    public int getTableSize(final String name) throws IOException {
        m_lock.lock();
        try {
            writeString("getTableSize");
            writeString(name);
            return readInt();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Get a serialized KNIME table from the python workspace.
     *
     * @param name the name of the variable in the python workspace
     * @return the serialized table as bytearray
     * @throws IOException
     */
    public byte[] getTable(final String name) throws IOException {
        m_lock.lock();
        try {
            writeString("getTable");
            writeString(name);
            //success message is sent before table is transmitted
            m_messages.waitForSuccessMessage();
            return readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Get a chunk of a serialized KNIME table from the python workspace.
     *
     * @param name the name of the variable in the python workspace
     * @param start the starting row of the chunk
     * @param end the last row of the chunk
     * @return the serialized table as bytearray
     * @throws IOException
     */
    public byte[] getTableChunk(final String name, final int start, final int end) throws IOException {
        m_lock.lock();
        try {
            writeString("getTableChunk");
            writeString(name);
            writeInt(start);
            writeInt(end);
            //success message is sent before table is transmitted
            m_messages.waitForSuccessMessage();
            return readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Get a list of the variable names in the python workspace.
     *
     * @return the serialized list of variable names
     * @throws IOException
     */
    public byte[] listVariables() throws IOException {
        m_lock.lock();
        try {
            writeString("listVariables");
            return readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Reset the python workspace by emptying the variable definitions.
     *
     * @throws IOException
     */
    public void reset() throws IOException {
        m_lock.lock();
        try {
            writeString("reset");
            readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Indicates if python supports autocompletion.
     *
     * @return autocompletion yes/no
     * @throws IOException
     */
    public boolean hasAutoComplete() throws IOException {
        m_lock.lock();
        try {
            writeString("hasAutoComplete");
            return readInt() > 0;
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Get a list of autocompletion suggestions for the given source code snippet.
     *
     * @param sourceCode the source code snippet in which the auto completion should be done
     * @param line the line number in the snippet for which auto completion is requested
     * @param column the cursor position in the line
     * @return serialized list of autocompletion suggestions
     * @throws IOException
     */
    public byte[] autoComplete(final String sourceCode, final int line, final int column) throws IOException {
        m_lock.lock();
        try {
            writeString("autoComplete");
            writeString(sourceCode);
            writeInt(line);
            writeInt(column);
            return readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Get an image from the python workspace
     *
     * @param name the name of the variable in the python workspace
     * @return a serialized image
     * @throws IOException
     */
    public byte[] getImage(final String name) throws IOException {
        m_lock.lock();
        try {
            writeString("getImage");
            writeString(name);
            return readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Get a python object from the python workspace. The object consists of a pickled representation, a type and a
     * string representation.
     *
     * @param name the name of the variable in the python workspace
     * @return a serialized python object
     * @throws IOException
     */
    public byte[] getObject(final String name) throws IOException {
        m_lock.lock();
        try {
            writeString("getObject");
            writeString(name);
            return readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Put a python object into the python workspace. The object consists of a pickled representation, a type and a
     * string representation.
     *
     * @param name the name of the variable in the python workspace
     * @param object a serialized python object
     * @throws IOException
     */
    public void putObject(final String name, final byte[] object) throws IOException {
        m_lock.lock();
        try {
            writeString("putObject");
            writeString(name);
            writeBytes(object);
            readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Add a serializer for an extension type to the python workspace.
     *
     * @param id the extension id (in java)
     * @param type the python type identifier
     * @param path the path to the code file containing the serializer function
     * @throws IOException
     */
    public void addSerializer(final String id, final String type, final String path) throws IOException {
        m_lock.lock();
        try {
            writeString("addSerializer");
            writeString(id);
            writeString(type);
            writeString(path);
            readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Add a deserializer for an extension type to the python workspace.
     *
     * @param id the extension id (in java)
     * @param path the path to the code file containing the deserializer function
     * @throws IOException
     */
    public void addDeserializer(final String id, final String path) throws IOException {
        m_lock.lock();
        try {
            writeString("addDeserializer");
            writeString(id);
            writeString(path);
            readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Shut down the python kernel to properly end the connection. Waits 1s to get the lock
     *
     * @return shutdown successful yes / no
     *
     * @throws IOException
     * @throws InterruptedException
     */
     public synchronized boolean tryShutdown() throws IOException, InterruptedException {
        if (m_lock.tryLock(1, TimeUnit.SECONDS)) {
            try {
                writeString("shutdown");
                //Give some time to shutdown
                Thread.sleep(1000);
                return true;
            } finally {
                m_lock.unlock();
            }
        }
        return false;
    }

    /**
     * Send information on how to connect to a specific SQL database alongside a query to the python workspace.
     *
     * @param name the name of the variable in the python workspace
     * @param sql the serialized table containing the the entries: driver, jdbcurl, username, password, jars, query,
     *            dbidentifier
     * @throws IOException
     */
    public void putSql(final String name, final byte[] sql) throws IOException {
        m_lock.lock();
        try {
            writeString("putSql");
            writeString(name);
            writeBytes(sql);
            readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Gets a SQL query from the python workspace.
     *
     * @param name the name of the variable in the python workspace
     * @return the SQL query
     * @throws IOException
     */
    public String getSql(final String name) throws IOException {
        m_lock.lock();
        try {
            writeString("getSql");
            writeString(name);
            return readString();
        } finally {
            m_lock.unlock();
        }
    }

    /**
     * Transmit the paths to all custom module directories and make them available via the pythonpath.
     *
     * @param paths ';' separated list of directories
     * @throws IOException
     */
    public void addToPythonPath(final String paths) throws IOException {
        m_lock.lock();
        try {
            writeString("setCustomModulePaths");
            writeString(paths);
            readBytes();
        } finally {
            m_lock.unlock();
        }
    }

    private byte[] stringToBytes(final String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    private static String stringFromBytes(final byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] intToBytes(final int integer) {
        return ByteBuffer.allocate(4).putInt(integer).array();
    }

    /*private int intFromBytes(final byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }*/

    private void writeString(final String string) throws IOException {
        writeMessageBytes(stringToBytes(string), m_bufferedOutToServer);
    }

    private static String readString(final DataInputStream in, final int size) throws IOException {
        return stringFromBytes(readBytes(in, size));
    }

    private String readString() throws IOException {
        return stringFromBytes(readBytes());
    }

    private void writeInt(final int integer) throws IOException {
        writeMessageBytes(intToBytes(integer), m_bufferedOutToServer);
    }

    /*private int readInt() throws IOException {
        return intFromBytes(readMessageBytes(m_bufferedInFromServer));
    }*/

    private void writeBytes(final byte[] bytes) throws IOException {
        writeMessageBytes(bytes, m_bufferedOutToServer);
    }

    /*private byte[] readBytes() throws IOException {
        return readMessageBytes(m_bufferedInFromServer);
    }*/

    /**
     * Writes the given message size as 32 bit integer into the output stream.
     *
     * @param size The size to write
     * @param outputStream The stream to write to
     * @throws IOException If an error occured
     */
    private static void writeSize(final int size, final DataOutputStream outputStream) throws IOException {
        outputStream.write(ByteBuffer.allocate(4).putInt(size).array());
        outputStream.flush();
    }

    /**
     * Writes the given message to the output stream.
     *
     * @param bytes The message as byte array
     * @param outputStream The stream to write to
     * @throws IOException If an error occured
     */
    private static void writeMessageBytes(final byte[] bytes, final DataOutputStream outputStream) throws IOException {
        writeSize(bytes.length, outputStream);
        outputStream.write(bytes);
        outputStream.flush();
    }

    /**
     * Reads the next 32 bit from the input stream and interprets them as integer.
     *
     * @param inputReader The stream to read from
     * @return The read size
     * @throws IOException If an error occured
     */
    private static int readSize(final DataInputStream inputStream) throws IOException {
        final byte[] bytes = new byte[4];
        //long millis = System.currentTimeMillis();
        inputStream.readFully(bytes);
        //NodeLogger.getLogger("readSize").warn("Spent " + (System.currentTimeMillis() - millis) + "ms in readSize().");
        return ByteBuffer.wrap(bytes).getInt();
    }

    private int readInt() throws IOException {
        final byte[] bytes = new byte[4];
        m_bufferedInFromServer.readFully(bytes);
        return ByteBuffer.wrap(bytes).getInt();
    }

    private static int readInt(final DataInputStream in) throws IOException {
        final byte[] bytes = new byte[4];
        in.readFully(bytes);
        return ByteBuffer.wrap(bytes).getInt();
    }

    /**
     * Reads the next message from the input stream.
     *
     * @param inputStream The stream to read from
     * @return The message as byte array
     * @throws IOException If an error occured
     */
    private byte[] readBytes() throws IOException {
        int size = readSize(m_bufferedInFromServer);
        final byte[] bytes = new byte[size];
        m_bufferedInFromServer.readFully(bytes);
        return bytes;
    }

    /**
     * Reads the next message from the input stream.
     *
     * @param inputStream The stream to read from
     * @return The message as byte array
     * @throws IOException If an error occured
     */
    private static byte[] readBytes(final DataInputStream inputStream, final int size) throws IOException {
        final byte[] bytes = new byte[size];
        inputStream.readFully(bytes);
        return bytes;
    }

    /**
     * Commands-based implementation of {@link Messages}.
     */
    private static class CommandsMessages implements Messages {

        private static final String SUCCESS_COMMAND = "success";

        private static final String RESPONSE_SUFFIX = "_response";

        private static final PythonToJavaMessageHandler SUCCESS_HANDLER =
            new AbstractPythonToJavaMessageHandler(SUCCESS_COMMAND) {

                @Override
                protected void handle(final CommandMessage msg) {
                    // no op
                }
            };

        private final Commands m_commands;

        private final List<PythonToJavaMessageHandler> m_msgHandlers = new ArrayList<>();

        private final Deque<CommandMessage> m_unansweredRequests = new ArrayDeque<>();

        public CommandsMessages(final Commands commands) {
            m_commands = commands;
            registerMessageHandler(SUCCESS_HANDLER);
        }

        @Override
        public synchronized void registerMessageHandler(final PythonToJavaMessageHandler handler) {
            m_commands.m_lock.lock();
            try {
                if (!m_msgHandlers.contains(CheckUtils.checkNotNull(handler))) {
                    m_msgHandlers.add(handler);
                }
            } finally {
                m_commands.m_lock.unlock();
            }
        }

        @Override
        public synchronized void unregisterMessageHandler(final PythonToJavaMessageHandler handler) {
            m_commands.m_lock.lock();
            try {
                m_msgHandlers.remove(CheckUtils.checkNotNull(handler));
            } finally {
                m_commands.m_lock.unlock();
            }
        }

        @Override
        public synchronized void answer(final JavaToPythonResponse response) throws IOException {
            m_commands.m_lock.lock();
            try {
                if (m_unansweredRequests.peek() != CheckUtils.checkNotNull(response).getOriginalMessage()) {
                    if (!m_unansweredRequests.contains(response.getOriginalMessage())) {
                        throw new IllegalStateException(
                            "Request message from Python may only be answered once. Response: " + response);
                    }
                    throw new IllegalStateException(
                        "Only the most recent request message from Python may be answered. Response: " + response);
                }
                m_unansweredRequests.pop();
                m_commands.writeString(response.getOriginalMessage().getCommand() + RESPONSE_SUFFIX);
                m_commands.writeString(response.getReponse());
            } finally {
                m_commands.m_lock.unlock();
            }
        }

        private CommandMessage readMessage() throws IOException {
            int headerSize = readInt(m_commands.m_bufferedInFromServer);
            int payloadSize = readInt(m_commands.m_bufferedInFromServer);
            String header = readString(m_commands.m_bufferedInFromServer, headerSize);
            byte[] payload = readBytes(m_commands.m_bufferedInFromServer, payloadSize);

            return new CommandMessage(header, payload);
        }

        /**
         * Direct a {@link CommandMessage} to the appropriate registered {@link PythonToJavaMessageHandler}. If the
         * message is a request, it has to be answered by calling {@link #answer(JavaToPythonResponse)} exactly once.
         *
         * @param msg a message from the python process
         */
        private void handleMessage(final CommandMessage msg) throws IOException {
            boolean handled = false;
            if (msg.isRequest()) {
                m_unansweredRequests.push(msg);
            }
            for (PythonToJavaMessageHandler handler : m_msgHandlers) {
                try {
                    handled = handler.tryHandle(msg);
                } catch (Exception ex) {
                    throw new IOException(ex.getMessage(), ex);
                }
                if (handled) {
                    break;
                }
            }
            if (!handled) {
                throw new IllegalStateException("Python message was not handled. Command: " + msg.getCommand());
            }
            if (m_unansweredRequests.peek() == msg) {
                throw new IllegalStateException(
                    "Python request message was not answered. Command: " + msg.getCommand());
            }
        }

        /**
         * Waits for the Python process to signal termination of the most recent command's execution via a
         * {@link #SUCCESS_COMMAND success message}. Any other {@link CommandMessage} will be passed through to its
         * associated {@link PythonToJavaMessageHandler}.
         *
         * @throws IOException if any exception occurs during reading and handling messages
         */
        private void waitForSuccessMessage() throws IOException {
            CommandMessage msg;
            do {
                msg = readMessage();
                handleMessage(msg);
            } while (!msg.getCommand().equals(SUCCESS_COMMAND));
        }
    }

    private class CommandsHandler implements PythonToJavaMessageHandler {

        private final HashMap<Integer, ResponseTask<?>> m_responseMap = new HashMap<Integer, ResponseTask<?>>();

        public synchronized <T> Future<T> registerResponse(final CommandMessage msg, final Function<CommandMessage,T> response) {
            ResponseTask<T> f = new ResponseTask<T>(response);
            m_responseMap.put(msg.getId(), f);
            return f;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean tryHandle(final CommandMessage msg) throws Exception {
            if(m_responseMap.containsKey(msg.getId())) {
                m_responseMap.get(msg.getId()).run(msg);
                m_responseMap.remove(msg.getId());
                return true;
            }
            return false;
        }

        private class ResponseTask<T> implements Future<T> {

            private boolean m_canceled = false;

            private ReentrantLock m_lock;

            private Condition m_waitForCompletion;

            private Function<CommandMessage, T> m_function;

            private T m_result;

            public ResponseTask(final Function<CommandMessage, T> fun) {
                m_lock = new ReentrantLock();
                m_waitForCompletion = m_lock.newCondition();
                m_function = fun;
            }

            public void run(final CommandMessage msg) {
                m_lock.lock();
                try{
                    m_result = m_function.apply(msg);
                    m_waitForCompletion.signalAll();
                } finally {
                    m_lock.unlock();
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                m_canceled = true;
                if(mayInterruptIfRunning) {
                    m_waitForCompletion.signalAll();
                    return true;
                }
                return !m_lock.hasWaiters(m_waitForCompletion);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public T get() throws InterruptedException, ExecutionException {
                m_lock.lock();
                try{
                    while(!m_canceled && m_result == null) {
                        m_waitForCompletion.await();
                    }
                    if(m_canceled) {
                        throw new CancellationException();
                    }
                    return m_result;
                } finally {
                    m_lock.unlock();
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public T get(final long timeout, final TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
                m_lock.lock();
                try{
                    while(!m_canceled && m_result == null) {
                        m_waitForCompletion.await(timeout, unit);
                    }
                    if(m_canceled) {
                        throw new CancellationException();
                    }
                    if(m_result == null) {
                        throw new TimeoutException();
                    }
                    return m_result;
                } finally {
                    m_lock.unlock();
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isCancelled() {
                return m_canceled;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isDone() {
                return m_result != null;
            }

        }

    }

}
