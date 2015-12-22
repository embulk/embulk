package org.embulk.spi;

import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import org.msgpack.value.Value;
import org.embulk.spi.time.Timestamp;

public class PageReader
        implements AutoCloseable
{
    private final Schema schema;
    private final int[] columnOffsets;

    private Page page = SENTINEL;
    private Slice pageSlice = null;
    private int pageRecordCount = 0;

    private int readCount = 0;
    private int position;
    private final byte[] nullBitSet;

    private static final Page SENTINEL = Page.wrap(Buffer.wrap(new byte[4]));  // buffer().release() does nothing

    public PageReader(Schema schema)
    {
        this.schema = schema;
        this.columnOffsets = PageFormat.columnOffsets(schema);
        this.nullBitSet = new byte[PageFormat.nullBitSetSize(schema)];
    }

    public static int getRecordCount(Page page)
    {
        Buffer pageBuffer = page.buffer();
        Slice pageSlice = Slices.wrappedBuffer(pageBuffer.array(), pageBuffer.offset(), pageBuffer.limit());
        return pageSlice.getInt(0);  // see page format
    }

    public void setPage(Page page)
    {
        this.page.buffer().release();
        this.page = SENTINEL;

        Buffer pageBuffer = page.buffer();
        Slice pageSlice = Slices.wrappedBuffer(pageBuffer.array(), pageBuffer.offset(), pageBuffer.limit());

        pageRecordCount = pageSlice.getInt(0);  // see page format
        readCount = 0;
        position = PageFormat.PAGE_HEADER_SIZE;

        this.page = page;
        this.pageSlice = pageSlice;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public boolean isNull(Column column)
    {
        return isNull(column.getIndex());
    }

    public boolean isNull(int columnIndex)
    {
        return (nullBitSet[columnIndex >>> 3] & (1 << (columnIndex & 7))) != 0;
    }

    public boolean getBoolean(Column column)
    {
        // TODO check type?
        return getBoolean(column.getIndex());
    }

    public boolean getBoolean(int columnIndex)
    {
        return pageSlice.getByte(getOffset(columnIndex)) != (byte) 0;
    }

    public long getLong(Column column)
    {
        // TODO check type?
        return getLong(column.getIndex());
    }

    public long getLong(int columnIndex)
    {
        return pageSlice.getLong(getOffset(columnIndex));
    }

    public double getDouble(Column column)
    {
        // TODO check type?
        return getDouble(column.getIndex());
    }

    public double getDouble(int columnIndex)
    {
        return pageSlice.getDouble(getOffset(columnIndex));
    }

    public String getString(Column column)
    {
        // TODO check type?
        return getString(column.getIndex());
    }

    public String getString(int columnIndex)
    {
        int index = pageSlice.getInt(getOffset(columnIndex));
        return page.getStringReference(index);
    }

    public Timestamp getTimestamp(Column column)
    {
        // TODO check type?
        return getTimestamp(column.getIndex());
    }

    public Timestamp getTimestamp(int columnIndex)
    {
        int offset = getOffset(columnIndex);
        long sec = pageSlice.getLong(offset);
        int nsec = pageSlice.getInt(offset + 8);
        return Timestamp.ofEpochSecond(sec, nsec);
    }

    public Value getJson(Column column)
    {
        // TODO check type?
        return getJson(column.getIndex());
    }

    public Value getJson(int columnIndex)
    {
        int index = pageSlice.getInt(getOffset(columnIndex));
        return page.getValueReference(index);
    }

    private int getOffset(int columnIndex)
    {
        return position + columnOffsets[columnIndex];
    }

    public boolean nextRecord()
    {
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
    public void close()
    {
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
