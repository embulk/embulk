package org.embulk.spi.type;

import org.embulk.spi.time.Timestamp;

public class TimestampType
        extends AbstractType
{
    static final TimestampType TIMESTAMP = new TimestampType();

    private static final String DEFAULT_FORMAT = "%Y-%m-%d %H:%M:%S.%6N %z";

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

    @Deprecated
    public TimestampType withFormat(String format)
    {
        // TODO is this correct design...?
        return new TimestampType(format);
    }

    @Deprecated
    public String getFormat()
    {
        if (format == null) {
            return DEFAULT_FORMAT;
        } else {
            return format;
        }
    }
}
