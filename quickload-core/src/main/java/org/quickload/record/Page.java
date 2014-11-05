package org.quickload.record;

import java.util.ArrayList;
import java.util.List;
import org.quickload.buffer.Buffer;

public class Page
        extends Buffer
{
    // PageHeader
    // +---+
    // | 4 |
    // +---+
    // count (number of records)

    static final int PAGE_HEADER_SIZE = 4;

    // PageBuilder.setVariableLengthData and PageReader.readVariableLengthData
    // uses 4 bytes integer
    static final int VARIABLE_LENGTH_COLUMN_SIZE = 4;

    ////
    //
    // Row
    // +---------------+------------+--------------------+------------------------+
    // | total-var-len | nul bitset | column header data | column var-len data... |
    // +---------------+------------+--------------------+------------------------+
    //  |                                               | |                      |
    //  +-----------------------------------------------+ +----------------------+
    //                     fixed row data size             variable row data size

    // string:
    //   binary ref:       0 30-index (32-off 32-len)
    //   binary payload:   1 30-dataOff (32-dataLen)
    //   string ref:       2 30-index
    //   string payload:   3 30-dataOff (32-dataLen)
    //
    // binary:
    //   binary ref:       1 30-index (32-off 32-len)
    //   binary payload:   3 30-dataOff (32-dataLen)
    //

    private final List<String> stringReferences; // TODO ??
    // TODO private final List<byte[]> binaryReferences;

    protected Page(int size)
    {
        super(size);
        this.stringReferences = new ArrayList<String>();
    }

    public static Page allocate(int length)
    {
        return new Page(length);
    }

    @Override
    public Page limit(int newLimit)
    {
        super.limit(newLimit);
        return this;
    }

    public void clear()
    {
        stringReferences.clear();
    }

    static int nullBitSetSize(Schema schema)
    {
        return (schema.size() + 7) / 8;
    }

    static int rowHeaderSize(Schema schema)
    {
        return 4 + nullBitSetSize(schema);
    }

    static int totalColumnSize(Schema schema)
    {
        return rowHeaderSize(schema) + schema.getFixedStorageSize();
    }

    static int[] columnOffsets(Schema schema)
    {
        int[] offsets = new int[schema.size()];

        if (!schema.isEmpty()) {
            offsets[0] = rowHeaderSize(schema);
            for (int i=0; i < schema.size()-1; i++) {
                offsets[i+1] = offsets[i] + schema.getColumnType(i).getFixedStorageSize();
            }
        }

        return offsets;
    }

    public String getStringReference(int index)
    {
        return stringReferences.get(index);
    }

    public int addStringReference(String value)
    {
        int index = stringReferences.size();
        stringReferences.add(value);
        return index;
    }
}
