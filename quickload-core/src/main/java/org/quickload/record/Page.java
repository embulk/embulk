package org.quickload.record;

import java.util.List;
import com.google.common.collect.ImmutableList;
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
    // Record
    // +---------------+------------+--------------------+------------------------+
    // | total-var-len | nul bitset | column header data | column var-len data... |
    // +---------------+------------+--------------------+------------------------+
    //  |                                               | |                      |
    //  +-----------------------------------------------+ +----------------------+
    //                     fixed record data size             variable record data size

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

    private static final List<String> EMPTY_STRING_REFERENCES = ImmutableList.<String>of();

    private List<String> stringReferences;
    // TODO private List<byte[]> binaryReferences;

    protected Page(int size)
    {
        super(size);
        this.stringReferences = EMPTY_STRING_REFERENCES;
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

    public int getRecordCount()
    {
        return getInt(0);
    }

    static int nullBitSetSize(Schema schema)
    {
        return (schema.size() + 7) / 8;
    }

    static int recordHeaderSize(Schema schema)
    {
        return 4 + nullBitSetSize(schema);
    }

    static int totalColumnSize(Schema schema)
    {
        return recordHeaderSize(schema) + schema.getFixedStorageSize();
    }

    static int[] columnOffsets(Schema schema)
    {
        int[] offsets = new int[schema.size()];

        if (!schema.isEmpty()) {
            offsets[0] = recordHeaderSize(schema);
            for (int i=0; i < schema.size()-1; i++) {
                offsets[i+1] = offsets[i] + schema.getColumnType(i).getFixedStorageSize();
            }
        }

        return offsets;
    }

    public void setStringReferences(List<String> values)
    {
        this.stringReferences = values;
    }

    public String getStringReference(int index)
    {
        return stringReferences.get(index);
    }
}
