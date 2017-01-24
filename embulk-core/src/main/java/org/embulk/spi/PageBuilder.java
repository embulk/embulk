package org.embulk.spi;

import java.io.Serializable;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import org.msgpack.value.Value;
import org.msgpack.value.ImmutableValue;
import org.embulk.spi.time.Timestamp;

public class PageBuilder
        implements AutoCloseable
{
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
    private final BiMap<String, Integer> stringReferences = HashBiMap.create();
    private List<ImmutableValue> valueReferences = new ArrayList<>();
    private int referenceSize;
    private int nextVariableLengthDataOffset;

    public PageBuilder(BufferAllocator allocator, Schema schema, PageOutput output)
    {
        this.allocator = allocator;
        this.output = output;
        this.schema = schema;
        this.columnOffsets = PageFormat.columnOffsets(schema);
        this.nullBitSet = new byte[PageFormat.nullBitSetSize(schema)];
        Arrays.fill(nullBitSet, (byte) -1);
        this.fixedRecordSize = PageFormat.recordHeaderSize(schema) + PageFormat.totalColumnSize(schema);
        this.nextVariableLengthDataOffset = fixedRecordSize;
        newBuffer();
    }

    private void newBuffer()
    {
        this.buffer = allocator.allocate(PageFormat.PAGE_HEADER_SIZE + fixedRecordSize);
        this.bufferSlice = Slices.wrappedBuffer(buffer.array(), buffer.offset(), buffer.capacity());
        this.count = 0;
        this.position = PageFormat.PAGE_HEADER_SIZE;
        this.stringReferences.clear();
        this.valueReferences = new ArrayList<>();
        this.referenceSize = 0;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public void setNull(Column column)
    {
        setNull(column.getIndex());
    }

    public void setNull(int columnIndex)
    {
        nullBitSet[columnIndex >>> 3] |= (1 << (columnIndex & 7));
    }

    private void clearNull(int columnIndex)
    {
        nullBitSet[columnIndex >>> 3] &= ~(1 << (columnIndex & 7));
    }

    public void setBoolean(Column column, boolean value)
    {
        // TODO check type?
        setBoolean(column.getIndex(), value);
    }

    public void setBoolean(int columnIndex, boolean value)
    {
        bufferSlice.setByte(getOffset(columnIndex), value ? (byte) 1 : (byte) 0);
        clearNull(columnIndex);
    }

    public void setLong(Column column, long value)
    {
        // TODO check type?
        setLong(column.getIndex(), value);
    }

    public void setLong(int columnIndex, long value)
    {
        bufferSlice.setLong(getOffset(columnIndex), value);
        clearNull(columnIndex);
    }

    public void setDouble(Column column, double value)
    {
        // TODO check type?
        setDouble(column.getIndex(), value);
    }

    public void setDouble(int columnIndex, double value)
    {
        bufferSlice.setDouble(getOffset(columnIndex), value);
        clearNull(columnIndex);
    }

    public void setString(Column column, String value)
    {
        // TODO check type?
        setString(column.getIndex(), value);
    }

    public void setString(int columnIndex, String value)
    {
        if (value == null) {
            setNull(columnIndex);
            return;
        }

        Integer reuseIndex = stringReferences.get(value);
        if (reuseIndex != null) {
            bufferSlice.setInt(getOffset(columnIndex), reuseIndex);
        } else {
            int index = stringReferences.size();
            stringReferences.put(value, index);
            bufferSlice.setInt(getOffset(columnIndex), index);
            referenceSize += value.length() * 2 + 4;  // assuming size of char = size of byte * 2 + length
        }
        clearNull(columnIndex);
    }

    public void setJson(Column column, Value value)
    {
        // TODO check type?
        setJson(column.getIndex(), value);
    }

    public void setJson(int columnIndex, Value value)
    {
        if (value == null) {
            setNull(columnIndex);
            return;
        }

        int index = valueReferences.size();
        valueReferences.add(value.immutableValue());
        bufferSlice.setInt(getOffset(columnIndex), index);
        referenceSize += 256;  // TODO how to estimate size of the value?
        clearNull(columnIndex);
    }

    public void setTimestamp(Column column, Timestamp value)
    {
        // TODO check type?
        setTimestamp(column.getIndex(), value);
    }

    public void setTimestamp(int columnIndex, Timestamp value)
    {
        if (value == null) {
            setNull(columnIndex);
            return;
        }

        int offset = getOffset(columnIndex);
        bufferSlice.setLong(offset, value.getEpochSecond());
        bufferSlice.setInt(offset + 8, value.getNano());
        clearNull(columnIndex);
    }

    private int getOffset(int columnIndex)
    {
        return position + columnOffsets[columnIndex];
    }

    private static class StringReferenceSortComparator
            implements Comparator<Map.Entry<String, Integer>>, Serializable
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

    public void addRecord()
    {
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

    private void doFlush()
    {
        if (buffer != null && count > 0) {
            // write page header
            bufferSlice.setInt(0, count);
            buffer.limit(position);

            // flush page
            Page page = Page.wrap(buffer)
                .setStringReferences(getSortedStringReferences())
                .setValueReferences(valueReferences);
            buffer = null;
            bufferSlice = null;
            output.add(page);
        }
    }

    public void flush()
    {
        doFlush();
        if (buffer == null) {
            newBuffer();
        }
    }

    public void finish()
    {
        doFlush();
        output.finish();
    }

    @Override
    public void close()
    {
        if (buffer != null) {
            buffer.release();
            buffer = null;
            bufferSlice = null;
        }
        output.close();
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
