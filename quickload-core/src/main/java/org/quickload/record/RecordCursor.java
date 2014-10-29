package org.quickload.record;

public class RecordCursor
        implements AutoCloseable
{
    private final int[] columnOffsets;
    private final int fixedRecordSize;

    private Page page;
    private int count;
    private int recordCount;
    private int position;
    private final byte[] nullBitSet;

    RecordCursor(Schema schema)
    {
        this.columnOffsets = Page.columnOffsets(schema);
        this.nullBitSet = new byte[Page.nullBitSetSize(schema)];
        this.fixedRecordSize = 4 + nullBitSet.length + Page.totalColumnSize(schema);
    }

    @Override
    public void close()
    {
        if (this.page != null) {
            page.release();
            this.page = null;
        }
    }

    public void reset(Page page)
    {
        close();
        this.page = page;
        this.count = 0;
        this.recordCount = page.getInt(0);
        this.position = Page.PAGE_HEADER_SIZE;
    }

    public int getRecordCount()
    {
        return recordCount;
    }

    public boolean next()
    {
        if (page == null || recordCount <= count) {
            return false;
        }

        count++;
        if (count > 0) {
            int lastRecordSize = page.getInt(position);
            position += lastRecordSize;
        }
        page.getBytes(position + 4, nullBitSet, 0, nullBitSet.length);

        return true;
    }

    public String getStringReference(int index)
    {
        return page.getStringReference(index);
    }

    public boolean isNull(int columnIndex)
    {
        return (nullBitSet[columnIndex >>> 3] & (1 << (columnIndex & 7))) != 0;
    }

    public byte getByte(int columnIndex)
    {
        return page.getByte(position + columnOffsets[columnIndex]);
    }

    public short getShort(int columnIndex)
    {
        return page.getShort(position + columnOffsets[columnIndex]);
    }

    public int getInt(int columnIndex)
    {
        return page.getInt(position + columnOffsets[columnIndex]);
    }

    public long getLong(int columnIndex)
    {
        return page.getLong(position + columnOffsets[columnIndex]);
    }

    public float getFloat(int columnIndex)
    {
        return page.getFloat(position + columnOffsets[columnIndex]);
    }

    public double getDouble(int columnIndex)
    {
        return page.getDouble(position + columnOffsets[columnIndex]);
    }

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
}
