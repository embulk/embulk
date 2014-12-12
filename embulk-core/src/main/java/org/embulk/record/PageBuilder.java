package org.embulk.record;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.embulk.time.Timestamp;
import org.embulk.channel.PageOutput;

public class PageBuilder
        implements AutoCloseable
{
    private final PageAllocator allocator;
    private final PageOutput output;
    private final int[] columnOffsets;
    private final TypeWriter[] typeWriters;
    private final int fixedRecordSize;

    private Page page;
    private int count;
    private int position;
    private final byte[] nullBitSet;
    private final BiMap<String, Integer> stringReferences;
    private int stringReferenceSize;
    private int nextVariableLengthDataOffset;

    public PageBuilder(PageAllocator allocator, Schema schema, PageOutput output)
    {
        this.allocator = allocator;
        this.output = output;
        this.columnOffsets = Page.columnOffsets(schema);
        this.nullBitSet = new byte[Page.nullBitSetSize(schema)];
        this.fixedRecordSize = Page.recordHeaderSize(schema) + Page.totalColumnSize(schema);
        this.typeWriters = new TypeWriter[schema.getColumnCount()];
        for (Column column : schema.getColumns()) {
            typeWriters[column.getIndex()] = column.getType().newWriter(this, column);
        }

        this.page = allocator.allocatePage(Page.PAGE_HEADER_SIZE + fixedRecordSize);
        this.count = 0;
        this.position = Page.PAGE_HEADER_SIZE;
        this.nextVariableLengthDataOffset = fixedRecordSize;
        this.stringReferences = HashBiMap.create();
    }

    public void setNull(int columnIndex)
    {
        nullBitSet[columnIndex >>> 3] |= (1 << (columnIndex & 7));
    }

    // for TypeWriter
    void setBoolean(int columnIndex, boolean value)
    {
        page.setByte(getOffset(columnIndex), value ? (byte) 1 : (byte) 0);
    }

    // for TypeWriter
    void setLong(int columnIndex, long value)
    {
        page.setLong(getOffset(columnIndex), value);
    }

    // for TypeWriter
    void setDouble(int columnIndex, double value)
    {
        page.setDouble(getOffset(columnIndex), value);
    }

    // for TypeWriter
    void setString(int columnIndex, String value)
    {
        Integer reuseIndex = stringReferences.get(value);
        if (reuseIndex != null) {
            page.setInt(getOffset(columnIndex), reuseIndex);
        } else {
            int index = stringReferences.size();
            stringReferences.put(value, index);
            page.setInt(getOffset(columnIndex), index);
            stringReferenceSize += value.length() * 2 + 4;  // assuming size of char = size of byte * 2 + length
        }
    }

    // for TypeWriter
    void setTimestamp(int columnIndex, Timestamp value)
    {
        int offset = getOffset(columnIndex);
        page.setLong(offset, value.getEpochSecond());
        page.setInt(offset + 8, value.getNano());
    }

    private int getOffset(int columnIndex)
    {
        return position + columnOffsets[columnIndex];
    }

    private static class StringReferenceSortComparator
            implements Comparator<Map.Entry<String, Integer>>
    {
        @Override
        public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2)
        {
            return e1.getValue().compareTo(e2.getValue());
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof StringReferenceSortComparator;
        }
    }

    private List<String> getSortedStringReferences()
    {
        ArrayList<Map.Entry<String, Integer>> s = new ArrayList<>(stringReferences.entrySet());
        Collections.sort(s, new StringReferenceSortComparator());
        String[] array = new String[s.size()];
        for (int i=0; i < array.length; i++) {
            array[i] = s.get(i).getKey();
        }
        return Arrays.asList(array);
    }

    //// TODO implement if useful
    //public void setBooleanColumn(int columnIndex, boolean value)
    //{
    //    ((BooleanWriter) typeWriters[columnIndex]).write(value);
    //}

    //public void setLongColumn(int columnIndex, long value)
    //{
    //}

    //public void setDoubleColumn(int columnIndex, double value)
    //{
    //}

    //public void setStringColumn(int columnIndex, String value)
    //{
    //}

    //public void setTimestampColumn(int columnIndex, Timestamp value)
    //{
    //}

    public void visitColumns(RecordWriter visitor)
    {
        for (TypeWriter writer : typeWriters) {
            writer.callRecordWriter(visitor);
        }
    }

    public void addRecord(RecordWriter visitor)
    {
        visitColumns(visitor);
        addRecord();
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

        // flush if next record will not fit in this page
        if (page.capacity() < position + nextVariableLengthDataOffset + stringReferenceSize) {
            flush();
        }
    }

    public void flush()
    {
        if (page != null && count > 0) {
            // page header
            page.setInt(0, count);
            page.setStringReferences(getSortedStringReferences());
            page.limit(position);

            output.add(page);
            this.page = allocator.allocatePage(Page.PAGE_HEADER_SIZE + fixedRecordSize);
            this.count = 0;
            this.position = Page.PAGE_HEADER_SIZE;
            this.stringReferences.clear();
            this.stringReferenceSize = 0;
        }
    }

    @Override
    public void close()
    {
        // similar to flush but doesn't allocate next page
        if (page != null) {
            if (count > 0) {
                // page header
                page.setInt(0, count);
                page.setStringReferences(getSortedStringReferences());
                page.limit(position);

                output.add(page);
            } else {
                page.release();
            }
            page = null;
            this.count = 0;
            this.position = Page.PAGE_HEADER_SIZE;
            this.stringReferences.clear();
            this.stringReferenceSize = 0;
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
