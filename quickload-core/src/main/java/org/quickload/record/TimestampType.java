package org.quickload.record;

import org.quickload.time.Timestamp;

public class TimestampType
        extends AbstractType
{
    static final TimestampType TIMESTAMP = new TimestampType();

    private static final String DEFAULT_FORMAT = "%Y-%m-%d %H-%M-%S,%N %z";

    private final String format;

    private TimestampType()
    {
        this(null);
    }

    private TimestampType(String format)
    {
        super("timestamp", Timestamp.class, 12);  // long msec + int nsec
        this.format = format;
    }

    public TimestampType withFormat(String format)
    {
        // TODO is this correct design...?
        return new TimestampType(format);
    }

    public String getFormat()
    {
        if (format == null) {
            return DEFAULT_FORMAT;
        } else {
            return format;
        }
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

    // TODO serialize / deserialize format
}
