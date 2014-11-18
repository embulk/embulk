package org.quickload.record;

public class StringWriter
        extends TypeWriter
{
    public StringWriter(PageBuilder builder, Column column)
    {
        super(builder, column);
    }

    public void writeNull()
    {
        builder.setNull(column.getIndex());
    }

    public void write(String value)
    {
        if (value == null) {
            // this is unnecessary check but exists for buggy plugins
            writeNull();
        } else {
            Page page = builder.getPage();
            int index = page.addStringReference(value);
            page.setInt(builder.getOffset(column.getIndex()), index);
        }
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeString(column, this);
    }
}
