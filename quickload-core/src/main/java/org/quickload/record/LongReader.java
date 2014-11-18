package org.quickload.record;

public class LongReader
        extends TypeReader
{
    private final PageReader reader;
    private final Column column;

    public LongReader(PageReader reader, Column column)
    {
        this.reader = reader;
        this.column = column;
    }

    @Override
    void callRecordReader(RecordReader visitor)
    {
        if (reader.isNull(column.getIndex())) {
            visitor.readNull(column);
        } else {
            visitor.readLong(column, reader.getPage().getLong(reader.getOffset(column.getIndex())));
        }
    }
}
