package org.embulk.spi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.embulk.deps.buffer.Slice;
import org.embulk.spi.json.JsonValue;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;
import org.msgpack.value.Value;

public class PageBuilderImpl extends PageBuilder {
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
    private List<JsonValue> jsonValueReferences = new ArrayList<>();
    private int referenceSize;
    private int nextVariableLengthDataOffset;

    public PageBuilderImpl(BufferAllocator allocator, Schema schema, PageOutput output) {
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
        this.bufferSlice = Slice.createWithWrappedBuffer(buffer);
        this.count = 0;
        this.position = PageFormat.PAGE_HEADER_SIZE;
        this.stringReferences = new ArrayList<>();
        this.jsonValueReferences = new ArrayList<>();
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

    @Deprecated
    @Override
    @SuppressWarnings("deprecation")
    public void setJson(Column column, Value value) {
        this.setJson(column, JsonValue.fromMsgpack(value));
    }

    @Override
    public void setJson(final Column column, final JsonValue value) {
        // TODO check type?
        setJson(column.getIndex(), value);
    }

    @Deprecated
    @Override
    @SuppressWarnings("deprecation")
    public void setJson(int columnIndex, Value value) {
        this.setJson(columnIndex, JsonValue.fromMsgpack(value));
    }

    @Override
    public void setJson(final int columnIndex, final JsonValue value) {
        if (value == null) {
            setNull(columnIndex);
        } else {
            row.setJson(columnIndex, value);
        }
    }

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void setTimestamp(Column column, org.embulk.spi.time.Timestamp value) {
        // TODO check type?
        this.setTimestamp(column, value.getInstant());
    }

    public void setTimestamp(final Column column, final Instant value) {
        // TODO check type?
        this.setTimestamp(column.getIndex(), value);
    }

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public void setTimestamp(int columnIndex, org.embulk.spi.time.Timestamp value) {
        this.setTimestamp(columnIndex, value.getInstant());
    }

    public void setTimestamp(final int columnIndex, final Instant value) {
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

    private void writeJson(final int columnIndex, final JsonValue value) {
        final int index = this.jsonValueReferences.size();
        this.jsonValueReferences.add(value);
        this.bufferSlice.setInt(this.getOffset(columnIndex), index);
        this.referenceSize += value.presumeReferenceSizeInBytes();
        this.clearNull(columnIndex);
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    private void writeTimestamp(int columnIndex, Instant value) {
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
            final PageImpl page = PageImpl.wrap(buffer)
                    .setStringReferencesInternal(this.stringReferences)
                    .setJsonValueReferencesInternal(this.jsonValueReferences);
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
     * |Row| works as a buffer against plugins that may add values incorrectly without |PageBuilderImpl#addRecord|.
     * It accepts just one value per column while |PageBuilderImpl| can double-store values regardless of columns.
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

        private void setJson(int columnIndex, final JsonValue value) {
            values[columnIndex].setJson(value);
        }

        private void setTimestamp(int columnIndex, Instant value) {
            values[columnIndex].setTimestamp(value);
        }

        private void write(PageBuilderImpl pageBuilder) {
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

        void setJson(JsonValue value);

        void setTimestamp(Instant value);

        void setNull();

        void write(PageBuilderImpl pageBuilder);
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

        public void setJson(final JsonValue value) {
            throw new IllegalStateException("Not reach here");
        }

        public void setTimestamp(Instant value) {
            throw new IllegalStateException("Not reach here");
        }

        public void setNull() {
            isNull = true;
        }

        public void write(PageBuilderImpl pageBuilder) {
            if (!isNull) {
                writeNotNull(pageBuilder);
            } else {
                pageBuilder.writeNull(column.getIndex());
            }
        }

        protected abstract void writeNotNull(PageBuilderImpl pageBuilder);
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
        public void setLong(final long value) {
            throw new IllegalStateException("Setting a LONG value to a BOOLEAN column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setDouble(final double value) {
            throw new IllegalStateException("Setting a DOUBLE value to a BOOLEAN column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setString(final String value) {
            throw new IllegalStateException("Setting a STRING value to a BOOLEAN column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setJson(final JsonValue value) {
            throw new IllegalStateException("Setting a JSON value to a BOOLEAN column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setTimestamp(final Instant value) {
            throw new IllegalStateException("Setting a TIMESTAMP value to a BOOLEAN column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void writeNotNull(PageBuilderImpl pageBuilder) {
            pageBuilder.writeBoolean(column.getIndex(), value);
        }
    }

    private static class LongColumnValue extends AbstractColumnValue {
        private long value;

        LongColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setBoolean(final boolean value) {
            throw new IllegalStateException("Setting a BOOLEAN value to a LONG column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setLong(long value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void setDouble(final double value) {
            throw new IllegalStateException("Setting a DOUBLE value to a LONG column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setString(final String value) {
            throw new IllegalStateException("Setting a STRING value to a LONG column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setJson(final JsonValue value) {
            throw new IllegalStateException("Setting a JSON value to a LONG column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setTimestamp(final Instant value) {
            throw new IllegalStateException("Setting a TIMESTAMP value to a LONG column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void writeNotNull(PageBuilderImpl pageBuilder) {
            pageBuilder.writeLong(column.getIndex(), value);
        }
    }

    private static class DoubleColumnValue extends AbstractColumnValue {
        private double value;

        DoubleColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setBoolean(final boolean value) {
            throw new IllegalStateException("Setting a BOOLEAN value to a DOUBLE column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setLong(final long value) {
            throw new IllegalStateException("Setting a LONG value to a DOUBLE column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setDouble(double value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void setString(final String value) {
            throw new IllegalStateException("Setting a STRING value to a DOUBLE column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setJson(final JsonValue value) {
            throw new IllegalStateException("Setting a JSON value to a DOUBLE column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setTimestamp(final Instant value) {
            throw new IllegalStateException("Setting a TIMESTAMP value to a DOUBLE column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void writeNotNull(PageBuilderImpl pageBuilder) {
            pageBuilder.writeDouble(column.getIndex(), value);
        }
    }

    private static class StringColumnValue extends AbstractColumnValue {
        private String value;

        StringColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setBoolean(final boolean value) {
            throw new IllegalStateException("Setting a BOOLEAN value to a STRING column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setLong(final long value) {
            throw new IllegalStateException("Setting a LONG value to a STRING column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setDouble(final double value) {
            throw new IllegalStateException("Setting a DOUBLE value to a STRING column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setString(String value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void setJson(final JsonValue value) {
            throw new IllegalStateException("Setting a JSON value to a STRING column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setTimestamp(final Instant value) {
            throw new IllegalStateException("Setting a TIMESTAMP value to a STRING column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void writeNotNull(PageBuilderImpl pageBuilder) {
            pageBuilder.writeString(column.getIndex(), value);
        }
    }

    private static class JsonColumnValue extends AbstractColumnValue {
        private JsonValue value;

        JsonColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setBoolean(final boolean value) {
            throw new IllegalStateException("Setting a BOOLEAN value to a JSON column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setLong(final long value) {
            throw new IllegalStateException("Setting a LONG value to a JSON column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setDouble(final double value) {
            throw new IllegalStateException("Setting a DOUBLE value to a JSON column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setString(final String value) {
            throw new IllegalStateException("Setting a STRING value to a JSON column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setJson(final JsonValue value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void setTimestamp(final Instant value) {
            throw new IllegalStateException("Setting a TIMESTAMP value to a JSON column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void writeNotNull(PageBuilderImpl pageBuilder) {
            pageBuilder.writeJson(column.getIndex(), value);
        }
    }

    private static class TimestampColumnValue extends AbstractColumnValue {
        private Instant value;

        TimestampColumnValue(Column column) {
            super(column);
        }

        @Override
        public void setBoolean(final boolean value) {
            throw new IllegalStateException("Setting a BOOLEAN value to a TIMESTAMP column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setLong(final long value) {
            throw new IllegalStateException("Setting a LONG value to a TIMESTAMP column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setDouble(final double value) {
            throw new IllegalStateException("Setting a DOUBLE value to a TIMESTAMP column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setString(final String value) {
            throw new IllegalStateException("Setting a STRING value to a TIMESTAMP column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setJson(final JsonValue value) {
            throw new IllegalStateException("Setting a JSON value to a TIMESTAMP column: " + this.column.getName() + ", " + this.column.getType());
        }

        @Override
        public void setTimestamp(Instant value) {
            this.value = value;
            this.isNull = false;
        }

        @Override
        public void writeNotNull(PageBuilderImpl pageBuilder) {
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
