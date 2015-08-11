package org.embulk.spi.util;

import java.util.List;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;
import org.embulk.spi.Page;
import org.embulk.spi.Schema;
import org.embulk.spi.Column;
import org.embulk.spi.PageReader;
import org.embulk.spi.type.TypeSource;
import org.embulk.spi.type.ValueConsumer;

public class SwitchPageReader
    implements AutoCloseable
{
    private static class TypeSourceCaller
    {
        private final TypeSource source;
        private final ValueConsumer consumer;

        public TypeSourceCaller(TypeSource source, ValueConsumer consumer)
        {
            this.source = source;
            this.consumer = consumer;
        }

        public void call()
        {
            source.getTo(consumer);
        }
    }

    private final PageReader pageReader;
    private final ValueSwitch consumer;
    private final List<TypeSourceCaller> callers;

    public SwitchPageReader(Schema schema, ValueSwitch consumer)
    {
        this.pageReader = new PageReader(schema);
        this.consumer = consumer;
        this.callers = ImmutableList.copyOf(Lists.transform(
                    schema.getColumns(), new Function<Column, TypeSourceCaller>()
                    {
                        public TypeSourceCaller apply(Column column)
                        {
                            return findSwitchCaseCaller(column, pageReader, SwitchPageReader.this.consumer);
                        }
                    }
                ));
    }

    public void read(Page page)
    {
        pageReader.setPage(page);
        while (pageReader.nextRecord()) {
            for (TypeSourceCaller caller : callers) {
                caller.call();
            }
        }
    }

    @Override
    public void close()
    {
        pageReader.close();
    }

    private static TypeSourceCaller findSwitchCaseCaller(Column column, PageReader pageReader, ValueSwitch consumer)
    {
        TypeSource source = column.getType().newSource(pageReader, column);
        return new TypeSourceCaller(source, consumer);
    }
}
