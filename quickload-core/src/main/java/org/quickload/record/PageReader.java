package org.quickload.record;

import java.util.Iterator;

public class PageReader
        implements AutoCloseable
{
    private final Schema schema;
    private final Iterator<Page> input;
    private final int[] columnOffsets;

    private Page page;
    private int pageRecordCount;
    private int readCount;
    private int position;
    private final byte[] nullBitSet;
    private final TypeReader[] typeReaders;

    public PageReader(Schema schema, Iterable<Page> input)
    {
        this.schema = schema;
        this.input = input.iterator();
        this.columnOffsets = Page.columnOffsets(schema);
        this.nullBitSet = new byte[Page.nullBitSetSize(schema)];
        this.typeReaders = new TypeReader[schema.getColumnCount()];
        for (Column column : schema.getColumns()) {
            typeReaders[column.getIndex()] = column.getType().newReader(this, column);
        }
    }

    public Schema getSchema()
    {
        return schema;
    }

    public boolean isNull(int columnIndex)
    {
        return (nullBitSet[columnIndex >>> 3] & (1 << (columnIndex & 7))) != 0;
    }

    // for TypeReader
    int getOffset(int columnIndex)
    {
        return position + columnOffsets[columnIndex];
    }

    // for TypeWriter
    Page getPage()
    {
        return page;
    }

    //// TODO implement if useful
    //public boolean getBooleanColumn(int columnIndex)
    //{
    //}

    //public long getLongColumn(int columnIndex)
    //{
    //}

    //public double getDoubleColumn(int columnIndex)
    //{
    //}

    //public String getStringColumn(int columnIndex)
    //{
    //}

    //public Timestamp getTimestampColumn(int columnIndex)
    //{
    //}

    public void visitColumns(RecordReader visitor)
    {
        for (TypeReader reader : typeReaders) {
            reader.callRecordReader(visitor);
        }
    }

    public boolean nextRecord()
    {
        if (page == null || pageRecordCount <= readCount) {
            if (!nextPage()) {
                return false;
            }
        }

        if (readCount > 0) {
            int lastRecordSize = page.getInt(position);
            position += lastRecordSize;
        }

        readCount++;
        page.getBytes(position + 4, nullBitSet, 0, nullBitSet.length);

        return true;
    }

    private boolean nextPage()
    {
        if (page != null) {
            page.release();
            page = null;
        }
        if (!input.hasNext()) {
            return false;
        }
        page = input.next();
        readCount = 0;
        pageRecordCount = page.getRecordCount();
        position = Page.PAGE_HEADER_SIZE;
        return true;
    }

    @Override
    public void close()
    {
        if (page != null) {
            page.release();
            page = null;
        }
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
