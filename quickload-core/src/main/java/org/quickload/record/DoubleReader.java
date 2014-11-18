package org.quickload.record;

public class DoubleReader
        extends TypeReader
{
    private final PageReader reader;
    private final Column column;

    public DoubleReader(PageReader reader, Column column)
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
            visitor.readDouble(column, reader.getPage().getDouble(reader.getOffset(column.getIndex())));
        }
    }
}
