package org.quickload.record;

public class StringWriter
        extends TypeWriter
{
    private final PageBuilder builder;
    private final Column column;

    public StringWriter(PageBuilder builder, Column column)
    {
        this.builder = builder;
        this.column = column;
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
