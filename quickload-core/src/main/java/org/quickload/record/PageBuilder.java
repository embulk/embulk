package org.quickload.record;

import java.util.Arrays;
import org.quickload.channel.PageOutput;

public class PageBuilder
{
    private final PageAllocator allocator;
    private final PageOutput output;
    private final int[] columnOffsets;
    private final int fixedRecordSize;

    private Page page;
    private int count;
    private int position;
    private final byte[] nullBitSet;
    private int nextVariableLengthDataOffset;

    public PageBuilder(PageAllocator allocator, Schema schema, PageOutput output)
    {
        this.allocator = allocator;
        this.output = output;
        this.columnOffsets = Page.columnOffsets(schema);
        this.nullBitSet = new byte[Page.nullBitSetSize(schema)];
        this.fixedRecordSize = 4 + nullBitSet.length + Page.totalColumnSize(schema);

        this.page = allocator.allocatePage(Page.PAGE_HEADER_SIZE + fixedRecordSize);
        this.count = 0;
        this.position = Page.PAGE_HEADER_SIZE;
    }

    public void addRecord()
    {
        // record header
        page.setInt(position, nextVariableLengthDataOffset);  // nextVariableLengthDataOffset means record size
        page.setBytes(position + 4, nullBitSet);

        count++;
        this.position += nextVariableLengthDataOffset;
        this.nextVariableLengthDataOffset = fixedRecordSize;
        Arrays.fill(nullBitSet, (byte) 0);
    }

    public void flush()
    {
        if (page != null) {
            // page header
            page.setInt(0, count);

            output.add(page);
            this.page = allocator.allocatePage(Page.PAGE_HEADER_SIZE + fixedRecordSize);
            this.count = 0;
            this.position = Page.PAGE_HEADER_SIZE;
        }
    }

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

    public void setNull(int columnIndex)
    {
        nullBitSet[columnIndex >>> 3] |= (1 << (columnIndex & 7));
    }

    public void setByte(int columnIndex, byte value)
    {
        page.setByte(position + columnOffsets[columnIndex], value);
    }

    public void setShort(int columnIndex, short value)
    {
        page.setShort(position + columnOffsets[columnIndex], value);
    }

    public void setInt(int columnIndex, int value)
    {
        page.setInt(position + columnOffsets[columnIndex], value);
    }

    public void setLong(int columnIndex, long value)
    {
        page.setLong(position + columnOffsets[columnIndex], value);
    }

    public void setFloat(int columnIndex, float value)
    {
        page.setFloat(position + columnOffsets[columnIndex], value);
    }

    public void setDouble(int columnIndex, double value)
    {
        page.setDouble(position + columnOffsets[columnIndex], value);
    }

    public int addStringReference(String value)
    {
        return page.addStringReference(value);
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
        if (page.length() < position + requiredOffsetFromPosition) {
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
}
