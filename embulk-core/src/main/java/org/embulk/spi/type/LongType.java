package org.embulk.spi.type;

import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageReader;

public class LongType
        extends AbstractType
{
    static final LongType LONG = new LongType();

    private LongType()
    {
        super("long", long.class, 8);
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

        public void setLong(long value)
        {
            pageBuilder.setLong(column, value);
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
                consumer.whenLong(column, pageReader.getLong(column));
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
                producer.whenLong(context, column, sink);
            }
        };
    }

    @Override
    public TypeSource newSource(PageReader pageReader, Column column)
    {
        return new Source(pageReader, column);
    }
}
