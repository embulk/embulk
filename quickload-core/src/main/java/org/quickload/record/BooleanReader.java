package org.quickload.record;

public class BooleanReader
        extends TypeReader
{
    private final PageReader reader;
    private final Column column;

    public BooleanReader(PageReader reader, Column column)
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
            visitor.readBoolean(column, reader.getPage().getByte(reader.getOffset(column.getIndex())) == (byte) 1 ? true : false);
        }
    }
}
