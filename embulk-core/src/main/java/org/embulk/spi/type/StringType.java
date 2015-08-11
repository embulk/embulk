package org.embulk.spi.type;

import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageReader;

public class StringType
        extends AbstractType
{
    static final StringType STRING = new StringType();

    private StringType()
    {
        super("string", String.class, 4);
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

        public void setString(String value)
        {
            pageBuilder.setString(column, value);
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
                consumer.whenString(column, pageReader.getString(column));
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
                producer.whenString(context, column, sink);
            }
        };
    }

    @Override
    public TypeSource newSource(PageReader pageReader, Column column)
    {
        return new Source(pageReader, column);
    }
}
