package org.embulk.spi;

import org.embulk.type.Schema;

abstract class PageFormat
{
    // PageHeader
    // +---+
    // | 4 |
    // +---+
    // count (number of records)

    private PageFormat() { }

    static final int PAGE_HEADER_SIZE = 4;

    // PageBuilder.setVariableLengthData and PageReader.readVariableLengthData
    // uses 4 bytes integer
    static final int VARIABLE_LENGTH_COLUMN_SIZE = 4;

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
}
