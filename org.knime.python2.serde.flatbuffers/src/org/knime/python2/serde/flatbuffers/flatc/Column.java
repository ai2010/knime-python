// automatically generated by the FlatBuffers compiler, do not modify

package org.knime.python2.serde.flatbuffers.flatc;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class Column extends Table {
  public static Column getRootAsColumn(ByteBuffer _bb) { return getRootAsColumn(_bb, new Column()); }
  public static Column getRootAsColumn(ByteBuffer _bb, Column obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public Column __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public int type() { int o = __offset(4); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public ByteColumn byteColumn() { return byteColumn(new ByteColumn()); }
  public ByteColumn byteColumn(ByteColumn obj) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public ByteCollectionColumn byteListColumn() { return byteListColumn(new ByteCollectionColumn()); }
  public ByteCollectionColumn byteListColumn(ByteCollectionColumn obj) { int o = __offset(8); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public ByteCollectionColumn byteSetColumn() { return byteSetColumn(new ByteCollectionColumn()); }
  public ByteCollectionColumn byteSetColumn(ByteCollectionColumn obj) { int o = __offset(10); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public BooleanColumn booleanColumn() { return booleanColumn(new BooleanColumn()); }
  public BooleanColumn booleanColumn(BooleanColumn obj) { int o = __offset(12); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public BooleanCollectionColumn booleanListColumn() { return booleanListColumn(new BooleanCollectionColumn()); }
  public BooleanCollectionColumn booleanListColumn(BooleanCollectionColumn obj) { int o = __offset(14); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public BooleanCollectionColumn booleanSetColumn() { return booleanSetColumn(new BooleanCollectionColumn()); }
  public BooleanCollectionColumn booleanSetColumn(BooleanCollectionColumn obj) { int o = __offset(16); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public DoubleColumn doubleColumn() { return doubleColumn(new DoubleColumn()); }
  public DoubleColumn doubleColumn(DoubleColumn obj) { int o = __offset(18); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public DoubleCollectionColumn doubleListColumn() { return doubleListColumn(new DoubleCollectionColumn()); }
  public DoubleCollectionColumn doubleListColumn(DoubleCollectionColumn obj) { int o = __offset(20); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public DoubleCollectionColumn doubleSetColumn() { return doubleSetColumn(new DoubleCollectionColumn()); }
  public DoubleCollectionColumn doubleSetColumn(DoubleCollectionColumn obj) { int o = __offset(22); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public IntColumn intColumn() { return intColumn(new IntColumn()); }
  public IntColumn intColumn(IntColumn obj) { int o = __offset(24); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public IntCollectionColumn intListColumn() { return intListColumn(new IntCollectionColumn()); }
  public IntCollectionColumn intListColumn(IntCollectionColumn obj) { int o = __offset(26); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public IntCollectionColumn intSetColumn() { return intSetColumn(new IntCollectionColumn()); }
  public IntCollectionColumn intSetColumn(IntCollectionColumn obj) { int o = __offset(28); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public LongColumn longColumn() { return longColumn(new LongColumn()); }
  public LongColumn longColumn(LongColumn obj) { int o = __offset(30); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public LongCollectionColumn longListColumn() { return longListColumn(new LongCollectionColumn()); }
  public LongCollectionColumn longListColumn(LongCollectionColumn obj) { int o = __offset(32); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public LongCollectionColumn longSetColumn() { return longSetColumn(new LongCollectionColumn()); }
  public LongCollectionColumn longSetColumn(LongCollectionColumn obj) { int o = __offset(34); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public StringColumn stringColumn() { return stringColumn(new StringColumn()); }
  public StringColumn stringColumn(StringColumn obj) { int o = __offset(36); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public StringCollectionColumn stringListColumn() { return stringListColumn(new StringCollectionColumn()); }
  public StringCollectionColumn stringListColumn(StringCollectionColumn obj) { int o = __offset(38); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }
  public StringCollectionColumn stringSetColumn() { return stringSetColumn(new StringCollectionColumn()); }
  public StringCollectionColumn stringSetColumn(StringCollectionColumn obj) { int o = __offset(40); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }

  public static int createColumn(FlatBufferBuilder builder,
      int type,
      int byteColumnOffset,
      int byteListColumnOffset,
      int byteSetColumnOffset,
      int booleanColumnOffset,
      int booleanListColumnOffset,
      int booleanSetColumnOffset,
      int doubleColumnOffset,
      int doubleListColumnOffset,
      int doubleSetColumnOffset,
      int intColumnOffset,
      int intListColumnOffset,
      int intSetColumnOffset,
      int longColumnOffset,
      int longListColumnOffset,
      int longSetColumnOffset,
      int stringColumnOffset,
      int stringListColumnOffset,
      int stringSetColumnOffset) {
    builder.startObject(19);
    Column.addStringSetColumn(builder, stringSetColumnOffset);
    Column.addStringListColumn(builder, stringListColumnOffset);
    Column.addStringColumn(builder, stringColumnOffset);
    Column.addLongSetColumn(builder, longSetColumnOffset);
    Column.addLongListColumn(builder, longListColumnOffset);
    Column.addLongColumn(builder, longColumnOffset);
    Column.addIntSetColumn(builder, intSetColumnOffset);
    Column.addIntListColumn(builder, intListColumnOffset);
    Column.addIntColumn(builder, intColumnOffset);
    Column.addDoubleSetColumn(builder, doubleSetColumnOffset);
    Column.addDoubleListColumn(builder, doubleListColumnOffset);
    Column.addDoubleColumn(builder, doubleColumnOffset);
    Column.addBooleanSetColumn(builder, booleanSetColumnOffset);
    Column.addBooleanListColumn(builder, booleanListColumnOffset);
    Column.addBooleanColumn(builder, booleanColumnOffset);
    Column.addByteSetColumn(builder, byteSetColumnOffset);
    Column.addByteListColumn(builder, byteListColumnOffset);
    Column.addByteColumn(builder, byteColumnOffset);
    Column.addType(builder, type);
    return Column.endColumn(builder);
  }

  public static void startColumn(FlatBufferBuilder builder) { builder.startObject(19); }
  public static void addType(FlatBufferBuilder builder, int type) { builder.addInt(0, type, 0); }
  public static void addByteColumn(FlatBufferBuilder builder, int byteColumnOffset) { builder.addOffset(1, byteColumnOffset, 0); }
  public static void addByteListColumn(FlatBufferBuilder builder, int byteListColumnOffset) { builder.addOffset(2, byteListColumnOffset, 0); }
  public static void addByteSetColumn(FlatBufferBuilder builder, int byteSetColumnOffset) { builder.addOffset(3, byteSetColumnOffset, 0); }
  public static void addBooleanColumn(FlatBufferBuilder builder, int booleanColumnOffset) { builder.addOffset(4, booleanColumnOffset, 0); }
  public static void addBooleanListColumn(FlatBufferBuilder builder, int booleanListColumnOffset) { builder.addOffset(5, booleanListColumnOffset, 0); }
  public static void addBooleanSetColumn(FlatBufferBuilder builder, int booleanSetColumnOffset) { builder.addOffset(6, booleanSetColumnOffset, 0); }
  public static void addDoubleColumn(FlatBufferBuilder builder, int doubleColumnOffset) { builder.addOffset(7, doubleColumnOffset, 0); }
  public static void addDoubleListColumn(FlatBufferBuilder builder, int doubleListColumnOffset) { builder.addOffset(8, doubleListColumnOffset, 0); }
  public static void addDoubleSetColumn(FlatBufferBuilder builder, int doubleSetColumnOffset) { builder.addOffset(9, doubleSetColumnOffset, 0); }
  public static void addIntColumn(FlatBufferBuilder builder, int intColumnOffset) { builder.addOffset(10, intColumnOffset, 0); }
  public static void addIntListColumn(FlatBufferBuilder builder, int intListColumnOffset) { builder.addOffset(11, intListColumnOffset, 0); }
  public static void addIntSetColumn(FlatBufferBuilder builder, int intSetColumnOffset) { builder.addOffset(12, intSetColumnOffset, 0); }
  public static void addLongColumn(FlatBufferBuilder builder, int longColumnOffset) { builder.addOffset(13, longColumnOffset, 0); }
  public static void addLongListColumn(FlatBufferBuilder builder, int longListColumnOffset) { builder.addOffset(14, longListColumnOffset, 0); }
  public static void addLongSetColumn(FlatBufferBuilder builder, int longSetColumnOffset) { builder.addOffset(15, longSetColumnOffset, 0); }
  public static void addStringColumn(FlatBufferBuilder builder, int stringColumnOffset) { builder.addOffset(16, stringColumnOffset, 0); }
  public static void addStringListColumn(FlatBufferBuilder builder, int stringListColumnOffset) { builder.addOffset(17, stringListColumnOffset, 0); }
  public static void addStringSetColumn(FlatBufferBuilder builder, int stringSetColumnOffset) { builder.addOffset(18, stringSetColumnOffset, 0); }
  public static int endColumn(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

