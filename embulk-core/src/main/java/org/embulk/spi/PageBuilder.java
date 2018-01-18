package org.embulk.spi;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;
import org.msgpack.value.ImmutableValue;
import org.msgpack.value.Value;

public class PageBuilder implements AutoCloseable {
    private final BufferAllocator allocator;
    private final PageOutput output;
    private final Schema schema;
    private final int[] columnOffsets;
    private final int fixedRecordSize;

    private Buffer buffer;
    private Slice bufferSlice;

    private int count;
    private int position;
    private final byte[] nullBitSet;
    private final Row row;
    private List<String> stringReferences = new ArrayList<>();
    private List<ImmutableValue> valueReferences = new ArrayList<>();
    private int referenceSize;
    private int nextVariableLengthDataOffset;

    public PageBuilder(BufferAllocator allocator, Schema schema, PageOutput output) {
        this.allocator = allocator;
        this.output = output;
        this.schema = schema;
        this.columnOffsets = PageFormat.columnOffsets(schema);
        this.nullBitSet = new byte[PageFormat.nullBitSetSize(schema)];
        Arrays.fill(nullBitSet, (byte) -1);
        this.row = Row.newRow(schema);
        this.fixedRecordSize = PageFormat.recordHeaderSize(schema) + PageFormat.totalColumnSize(schema);
        this.nextVariableLengthDataOffset = fixedRecordSize;
        newBuffer();
    }

    private void newBuffer() {
        this.buffer = allocator.allocate(PageFormat.PAGE_HEADER_SIZE + fixedRecordSize);
        this.bufferSlice = Slices.wrappedBuffer(buffer.array(), buffer.offset(), buffer.capacity());
        this.count = 0;
        this.position = PageFormat.PAGE_HEADER_SIZE;
        this.stringReferences = new ArrayList<>();
        this.valueReferences = new ArrayList<>();
        this.referenceSize = 0;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setNull(Column column) {
        setNull(column.getIndex());
    }

    public void setNull(int columnIndex) {
        row.setNull(columnIndex);
    }

    public void setBoolean(Column column, boolean value) {
        // TODO check type?
        setBoolean(column.getIndex(), value);
    }

    public void setBoolean(int columnIndex, boolean value) {
        row.setBoolean(columnIndex, value);
    }

    public void setLong(Column column, long value) {
        // TODO check type?
        setLong(column.getIndex(), value);
    }

    public void setLong(int columnIndex, long value) {
        row.setLong(columnIndex, value);
    }

    public void setDouble(Column column, double value) {
        // TODO check type?
        setDouble(column.getIndex(), value);
    }

    public void setDouble(int columnIndex, double value) {
        row.setDouble(columnIndex, value);
    }

    public void setString(Column column, String value) {
        // TODO check type?
        setString(column.getIndex(), value);
    }

    public void setString(int columnIndex, String value) {
        if (value == null) {
            setNull(columnIndex);
        } else {
            row.setString(columnIndex, value);
        }
    }

    public void setJson(Column column, Value value) {
        // TODO check type?
        setJson(column.getIndex(), value);
    }

    public void setJson(int columnIndex, Value value) {
        if (value == null) {
            setNull(columnIndex);
        } else {
            row.setJson(columnIndex, value);
        }
    }

    public void setTimestamp(Column column, Timestamp value) {
        // TODO check type?
        setTimestamp(column.getIndex(), value);
    }

    public void setTimestamp(int columnIndex, Timestamp value) {
        if (value == null) {
            setNull(columnIndex);
        } else {
            row.setTimestamp(columnIndex, value);
        }
    }

    private void writeNull(int columnIndex) {
        nullBitSet[columnIndex >>> 3] |= (1 << (columnIndex & 7));
    }

    private void clearNull(int columnIndex) {
        nullBitSet[columnIndex >>> 3] &= ~(1 << (columnIndex & 7));
    }

    private void writeBoolean(int columnIndex, boolean value) {
        bufferSlice.setByte(getOffset(columnIndex), value ? (byte) 1 : (byte) 0);
        clearNull(columnIndex);
    }

    private void writeLong(int columnIndex, long value) {
        bufferSlice.setLong(getOffset(columnIndex), value);
        clearNull(columnIndex);
    }

    private void writeDouble(int columnIndex, double value) {
        bufferSlice.setDouble(getOffset(columnIndex), value);
        clearNull(columnIndex);
    }

    private void writeString(int columnIndex, String value) {
        int index = stringReferences.size();
        stringReferences.add(value);
        bufferSlice.setInt(getOffset(columnIndex), index);
        referenceSize += value.length() * 2 + 4;  // assuming size of char = size of byte * 2 + length
        clearNull(columnIndex);
    }

    private void writeJson(int columnIndex, Value value) {
        int index = valueReferences.size();
        valueReferences.add(value.immutableValue());
        bufferSlice.setInt(getOffset(columnIndex), index);
        referenceSize += 256;  // TODO how to estimate size of the value?
        clearNull(columnIndex);
    }

    private void writeTimestamp(int columnIndex, Timestamp value) {
        int offset = getOffset(columnIndex);
        bufferSlice.setLong(offset, value.getEpochSecond());
        bufferSlice.setInt(offset + 8, value.getNano());
        clearNull(columnIndex);
    }

    private int getOffset(int columnIndex) {
        return position + columnOffsets[columnIndex];
    }

    public void addRecord() {
        // record
        row.write(this);

        // record header
        bufferSlice.setInt(position, nextVariableLengthDataOffset);  // nextVariableLengthDataOffset means record size
        bufferSlice.setBytes(position + 4, nullBitSet);
        count++;

        this.position += nextVariableLengthDataOffset;
        this.nextVariableLengthDataOffset = fixedRecordSize;
        Arrays.fill(nullBitSet, (byte) -1);

        // flush if next record will not fit in this buffer
        if (buffer.capacity() < position + nextVariableLengthDataOffset + referenceSize) {
            flush();
        }
    }

    private void doFlush() {
        if (buffer != null && count > 0) {
            // write page header
            bufferSlice.setInt(0, count);
            buffer.limit(position);

            // flush page
            Page page = Page.wrap(buffer)
                    .setStringReferences(stringReferences)
                    .setValueReferences(valueReferences);
            buffer = null;
            bufferSlice = null;
            output.add(page);
        }
    }

    public void flush() {
        doFlush();
        if (buffer == null) {
            newBuffer();
        }
    }

    public void finish() {
        doFlush();
        output.finish();
    }

    @Override
    public void close() {
        if (buffer != null) {
            buffer.release();
            buffer = null;
            bufferSlice = null;
        }
        output.close();
    }

    /**
     * Row is a container to stage values before adding into reference lists such as |stringReferences|.
     *
     * |Row| works as a buffer against plugins that may add values incorrectly without |PageBuilder#addRecord|.
     * It accepts just one value per column while |PageBuilder| can double-store values regardless of columns.
     * Double-stored values are overwritten.
     */
    private static class Row {
        private static Row newRow(Schema schema) {
            ColumnValue[] values = new ColumnValue[schema.getColumnCount()];
            for (Column column : schema.getColumns()) {
                values[column.getIndex()] = newValue(column);
            }
            return new Row(values);
        }

        private static ColumnValue newValue(Column column) {
            Type type = column.getType();
            if (type.equals(Types.BOOLEAN)) {
                return new BooleanColumnValue(column);
            } else if (type.equals(Types.DOUBLE)) {
                return new DoubleColumnValue(column);
            } else if (type.equals(Types.LONG)) {
                return new LongColumnValue(column);
            } else if (type.equals(Types.STRING)) {
                return new StringColumnValue(column);
            } else if (type.equals(Types.JSON)) {
                return new JsonColumnValue(column);
            } else if (type.equals(Types.TIMESTAMP)) {
                return new TimestampColumnValue(column);
            } else {
                throw new IllegalStateException("Unsupported type " + type.getName());
            }
        }

        private final ColumnValue[] values;

        private Row(ColumnValue[] values) {
            this.values = values;
        }

        private void setNull(int columnIndex) {
            values[columnIndex].setNull();
        }

        private void setBoolean(int columnIndex, boolean value) {
            values[columnIndex].setBoolean(value);
        }

        private void setLong(int columnIndex, long value) {
            values[columnIndex].setLong(value);
        }

        private void setDouble(int columnIndex, double value) {
            values[columnIndex].setDouble(value);
        }

        private void setString(int columnIndex, String value) {
            values[columnIndex].setString(value);
        }

        private void setJson(int columnIndex, Value value) {
            values[columnIndex].setJson(value);
        }

        private void setTimestamp(int columnIndex, Timestamp value) {
            values[columnIndex].setTimestamp(value);
        }

        private void write(PageBuilder pageBuilder) {
            for (ColumnValue v : values) {
                v.write(pageBuilder);
            }
        }
    }

    private interface ColumnValue {
        void setBoolean(boolean value);

        void setLong(long value);

        void setDouble(double value);

        void setString(String value);

        void setJson(Value value);

        void setTimestamp(Timestamp value);

        void setNull();

        void write(PageBuilder pageBuilder);
    }

    private abstract static class AbstractColumnValue implements ColumnValue {
        protected final Column column;
        protected boolean isNull;

        protected AbstractColumnValue(Column column) {
            this.column = column;
        }

        public void setBoolean(boolean value) {
            throw new IllegalStateException("Not reach here");
        }

        public void setLong(long value) {
            throw new IllegalStateException("Not reach here");
        }

        public void setDouble(double value) {
            throw new IllegalStateException("Not reach here");
        }

        public void setString(String value) {
            throw new IllegalStateException("Not reach here");
        }

        public void setJson(Value value) {
            throw new IllegalStateException("Not reach here");
        }

        public void setTimestamp(Timestamp value) {
            throw new IllegalStateException("Not reach here");
        }

        public void setNull() {
            isNull = true;
        }

        public void write(PageBuilder pageBuilder) {
            if (!isNull) {
                writeNotNull(pageBuilder);
            } else {
                pageBuilder.writeNull(column.getIndex());
            }
        }

        protected abstract void writeNotNull(PageBuilder pageBuilder);
    }

    private static class BooleanColumnValue extends AbstractColumnValue {
        private boolean value;

        BooleanColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setBoolean(boolean value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder) {
            pageBuilder.writeBoolean(column.getIndex(), value);
        }
    }

    private static class LongColumnValue extends AbstractColumnValue {
        private long value;

        LongColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setLong(long value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder) {
            pageBuilder.writeLong(column.getIndex(), value);
        }
    }

    private static class DoubleColumnValue extends AbstractColumnValue {
        private double value;

        DoubleColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setDouble(double value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder) {
            pageBuilder.writeDouble(column.getIndex(), value);
        }
    }

    private static class StringColumnValue extends AbstractColumnValue {
        private String value;

        StringColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setString(String value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder) {
            pageBuilder.writeString(column.getIndex(), value);
        }
    }

    private static class JsonColumnValue extends AbstractColumnValue {
        private Value value;

        JsonColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setJson(Value value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder) {
            pageBuilder.writeJson(column.getIndex(), value);
        }
    }

    private static class TimestampColumnValue extends AbstractColumnValue {
        private Timestamp value;

        TimestampColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setTimestamp(Timestamp value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilder pageBuilder) {
            pageBuilder.writeTimestamp(column.getIndex(), value);
        }
    }

    /* TODO for variable-length types
    private void flushAndTakeOverRemaingData()
    {
        if (page != null) {
            // page header
            page.setInt(0, count);

            Page lastPage = page;

            this.page = allocator.allocatePage(Page.PAGE_HEADER_SIZE + fixedRecordSize + nextVariableLengthDataOffset);
            page.setBytes(Page.PAGE_HEADER_SIZE, lastPage, position, nextVariableLengthDataOffset);
            this.count = 0;
            this.position = Page.PAGE_HEADER_SIZE;

            output.add(lastPage);
        }
    }

    public int getVariableLengthDataOffset()
    {
        return nextVariableLengthDataOffset;
    }

    public VariableLengthDataWriter setVariableLengthData(int columnIndex, int intData)
    {
        // Page.VARIABLE_LENGTH_COLUMN_SIZE is 4 bytes
        page.setInt(position + columnOffsets[columnIndex], intData);
        return new VariableLengthDataWriter(nextVariableLengthDataOffset);
    }

    Page ensureVariableLengthDataCapacity(int requiredOffsetFromPosition)
    {
        if (page.capacity() < position + requiredOffsetFromPosition) {
            flushAndTakeOverRemaingData();
        }
        return page;
    }

    public class VariableLengthDataWriter
    {
        private int offsetFromPosition;

        VariableLengthDataWriter(int offsetFromPosition)
        {
            this.offsetFromPosition = offsetFromPosition;
        }

        public void writeByte(byte value)
        {
            ensureVariableLengthDataCapacity(offsetFromPosition + 1);
            page.setByte(position + offsetFromPosition, value);
            offsetFromPosition += 1;
        }

        public void writeShort(short value)
        {
            ensureVariableLengthDataCapacity(offsetFromPosition + 2);
            page.setShort(position + offsetFromPosition, value);
            offsetFromPosition += 2;
        }

        public void writeInt(int value)
        {
            ensureVariableLengthDataCapacity(offsetFromPosition + 4);
            page.setInt(position + offsetFromPosition, value);
            offsetFromPosition += 4;
        }

        public void writeLong(long value)
        {
            ensureVariableLengthDataCapacity(offsetFromPosition + 8);
            page.setLong(position + offsetFromPosition, value);
            offsetFromPosition += 8;
        }

        public void writeFloat(float value)
        {
            ensureVariableLengthDataCapacity(offsetFromPosition + 4);
            page.setFloat(position + offsetFromPosition, value);
            offsetFromPosition += 4;
        }

        public void writeDouble(double value)
        {
            ensureVariableLengthDataCapacity(offsetFromPosition + 8);
            page.setDouble(position + offsetFromPosition, value);
            offsetFromPosition += 8;
        }

        public void writeBytes(byte[] data)
        {
            writeBytes(data, 0, data.length);
        }

        public void writeBytes(byte[] data, int off, int len)
        {
            ensureVariableLengthDataCapacity(offsetFromPosition + len);
            page.setBytes(position + offsetFromPosition, data, off, len);
            offsetFromPosition += len;
        }
    }
    */
}
