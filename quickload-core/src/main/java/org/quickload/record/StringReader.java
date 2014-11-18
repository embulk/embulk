package org.quickload.record;

public class StringReader
        extends TypeReader
{
    public StringReader(PageReader reader, Column column)
    {
        super(reader, column);
    }

    @Override
    void callRecordReader(RecordReader visitor)
    {
        if (reader.isNull(column.getIndex())) {
            visitor.readNull(column);
        } else {
            Page page = reader.getPage();
            int index = reader.getPage().getInt(reader.getOffset(column.getIndex()));
            String value = page.getStringReference(index);
            visitor.readString(column, value);
        }
    }
}
