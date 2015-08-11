package org.embulk.spi.type;

import org.embulk.spi.time.Timestamp;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageReader;

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

    public static class Sink
            implements TypeSink
    {
        private final PageBuilder pageBuilder;
        private final Column column;

        public Sink(PageBuilder pageBuilder, Column column)
        {
            this.pageBuilder = pageBuilder;
            this.column = column;
        }

        public void setNull()
        {
            pageBuilder.setNull(column);
        }

        public void setTimestamp(Timestamp value)
        {
            pageBuilder.setTimestamp(column, value);
        }
    }

    private static class Source
            implements TypeSource
    {
        private final PageReader pageReader;
        private final Column column;

        public Source(PageReader pageReader, Column column)
        {
            this.pageReader = pageReader;
            this.column = column;
        }

        @Override
        public void getTo(ValueConsumer consumer)
        {
            if (pageReader.isNull(column)) {
                consumer.whenNull(column);
            } else {
                consumer.whenTimestamp(column, pageReader.getTimestamp(column));
            }
        }
    }

    @Override
    public <C> TypeSinkCaller<C> newTypeSinkCaller(final ValueProducer<C> producer, PageBuilder pageBuilder, final Column column)
    {
        final Sink sink = new Sink(pageBuilder, column);
        return new TypeSinkCaller<C>() {
            @Override
            public void call(C context)
            {
                producer.whenTimestamp(context, column, sink);
            }
        };
    }

    @Override
    public TypeSource newSource(PageReader pageReader, Column column)
    {
        return new Source(pageReader, column);
    }
}
