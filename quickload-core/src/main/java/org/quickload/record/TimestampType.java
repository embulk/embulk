package org.quickload.record;

import java.sql.Timestamp;

public class TimestampType
        extends AbstractType
{
    static final TimestampType TIMESTAMP = new TimestampType();

    private TimestampType()
    {
        super("timestamp", Timestamp.class, 12);  // long msec + int nsec
    }

    @Override
    public TypeWriter newWriter(PageBuilder builder, Column column)
    {
        return new TimestampWriter(builder, column);
    }

    @Override
    public TypeReader newReader(PageReader reader, Column column)
    {
        return new TimestampReader(reader, column);
    }
}
