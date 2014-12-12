package org.embulk.record;

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
            builder.setString(column.getIndex(), value);
        }
    }

    @Override
    void callRecordWriter(RecordWriter visitor)
    {
        visitor.writeString(column, this);
    }
}
