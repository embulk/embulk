package org.embulk.spi;

import java.time.Instant;
import org.embulk.deps.buffer.Slice;
import org.msgpack.value.Value;

public class PageReaderImpl extends PageReader {
    private final Schema schema;
    private final int[] columnOffsets;

    private Page page = SENTINEL;
    private Slice pageSlice = null;
    private int pageRecordCount = 0;

    private int readCount = 0;
    private int position;
    private final byte[] nullBitSet;

    private static final Page SENTINEL = PageImpl.wrap(BufferImpl.wrap(new byte[4]));  // buffer().release() does nothing

    public PageReaderImpl(Schema schema) {
        this.schema = schema;
        this.columnOffsets = PageFormat.columnOffsets(schema);
        this.nullBitSet = new byte[PageFormat.nullBitSetSize(schema)];
    }

    public static int getRecordCount(Page page) {
        Buffer pageBuffer = page.buffer();
        Slice pageSlice = Slice.createWithWrappedBuffer(pageBuffer);
        return pageSlice.getInt(0);  // see page format
    }

    @Override
    public void setPage(Page page) {
        this.page.buffer().release();
        this.page = SENTINEL;

        Buffer pageBuffer = page.buffer();
        Slice pageSlice = Slice.createWithWrappedBuffer(pageBuffer);

        pageRecordCount = pageSlice.getInt(0);  // see page format
        readCount = 0;
        position = PageFormat.PAGE_HEADER_SIZE;

        this.page = page;
        this.pageSlice = pageSlice;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public boolean isNull(Column column) {
        return isNull(column.getIndex());
    }

    @Override
    public boolean isNull(int columnIndex) {
        return (nullBitSet[columnIndex >>> 3] & (1 << (columnIndex & 7))) != 0;
    }

    @Override
    public boolean getBoolean(Column column) {
        // TODO check type?
        return getBoolean(column.getIndex());
    }

    @Override
    public boolean getBoolean(int columnIndex) {
        return pageSlice.getByte(getOffset(columnIndex)) != (byte) 0;
    }

    @Override
    public long getLong(Column column) {
        // TODO check type?
        return getLong(column.getIndex());
    }

    @Override
    public long getLong(int columnIndex) {
        return pageSlice.getLong(getOffset(columnIndex));
    }

    @Override
    public double getDouble(Column column) {
        // TODO check type?
        return getDouble(column.getIndex());
    }

    @Override
    public double getDouble(int columnIndex) {
        return pageSlice.getDouble(getOffset(columnIndex));
    }

    @Override
    public String getString(Column column) {
        // TODO check type?
        return getString(column.getIndex());
    }

    @Override
    public String getString(int columnIndex) {
        if (isNull(columnIndex)) {
            return null;
        }
        int index = pageSlice.getInt(getOffset(columnIndex));
        return page.getStringReference(index);
    }

    /**
     * Returns a Timestamp value.
     *
     * @deprecated Use {@link #getTimestampInstant(Column)} instead.
     */
    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    @Override
    public org.embulk.spi.time.Timestamp getTimestamp(Column column) {
        // TODO check type?
        return org.embulk.spi.time.Timestamp.ofInstant(this.getTimestampInstant(column.getIndex()));
    }

    /**
     * Returns a Timestamp value.
     *
     * @deprecated Use {@link #getTimestampInstant(int)} instead.
     */
    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    @Override
    public org.embulk.spi.time.Timestamp getTimestamp(int columnIndex) {
        return org.embulk.spi.time.Timestamp.ofInstant(this.getTimestampInstant(columnIndex));
    }

    @Override
    public Instant getTimestampInstant(final Column column) {
        // TODO check type?
        return this.getTimestampInstant(column.getIndex());
    }

    @Override
    public Instant getTimestampInstant(final int columnIndex) {
        if (isNull(columnIndex)) {
            return null;
        }
        int offset = getOffset(columnIndex);
        long sec = pageSlice.getLong(offset);
        int nsec = pageSlice.getInt(offset + 8);
        return Instant.ofEpochSecond(sec, nsec);
    }

    @Override
    public Value getJson(Column column) {
        // TODO check type?
        return getJson(column.getIndex());
    }

    @Override
    public Value getJson(int columnIndex) {
        if (isNull(columnIndex)) {
            return null;
        }
        int index = pageSlice.getInt(getOffset(columnIndex));
        return page.getValueReference(index);
    }

    private int getOffset(int columnIndex) {
        return position + columnOffsets[columnIndex];
    }

    /**
     * Returns the lineage metadata at the current record.
     *
     * @since 0.10.38
     */
    // @Override
    public LineageMetadata getPageLineage() {
        return LineageMetadata.EMPTY;
    }

    /**
     * Returns the lineage metadata at the current record.
     *
     * @since 0.10.38
     */
    // @Override
    public LineageMetadata getRecordLineage() {
        return LineageMetadata.EMPTY;
    }

    @Override
    public boolean nextRecord() {
        if (pageRecordCount <= readCount) {
            return false;
        }

        if (readCount > 0) {
            // advance position excepting the first record
            int lastRecordSize = pageSlice.getInt(position);
            position += lastRecordSize;
        }

        readCount++;
        pageSlice.getBytes(position + 4, nullBitSet, 0, nullBitSet.length);

        return true;
    }

    @Override
    public void close() {
        page.buffer().release();
        page = SENTINEL;
    }

    /* TODO for variable-length types
    public VariableLengthDataReader getVariableLengthData(int columnIndex, int variableLengthDataOffset)
    {
        return new VariableLengthDataReader(variableLengthDataOffset);
    }

    public class VariableLengthDataReader
    {
        private int offsetFromPosition;

        VariableLengthDataReader(int offsetFromPosition)
        {
            this.offsetFromPosition = offsetFromPosition;
        }

        public byte readByte()
        {
            byte value = page.getByte(position + offsetFromPosition);
            offsetFromPosition += 1;
            return value;
        }

        public short readShort()
        {
            short value = page.getShort(position + offsetFromPosition);
            offsetFromPosition += 2;
            return value;
        }

        public int readInt()
        {
            int value = page.getInt(position + offsetFromPosition);
            offsetFromPosition += 4;
            return value;
        }

        public long readLong()
        {
            long value = page.getLong(position + offsetFromPosition);
            offsetFromPosition += 8;
            return value;
        }

        public float readFloat()
        {
            float value = page.getFloat(position + offsetFromPosition);
            offsetFromPosition += 4;
            return value;
        }

        public double readDouble()
        {
            double value = page.getDouble(position + offsetFromPosition);
            offsetFromPosition += 8;
            return value;
        }

        public void readBytes(byte[] data)
        {
            readBytes(data, 0, data.length);
        }

        public void readBytes(byte[] data, int off, int len)
        {
            page.getBytes(position + offsetFromPosition, data, off, len);
            offsetFromPosition += len;
        }
    }
    */
}
