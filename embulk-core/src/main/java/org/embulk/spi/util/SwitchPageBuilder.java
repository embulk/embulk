package org.embulk.spi.util;

import java.util.List;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;
import org.embulk.spi.Schema;
import org.embulk.spi.Column;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.type.TypeSinkCaller;
import org.embulk.spi.type.ValueProducer;

public class SwitchPageBuilder <C>
        implements AutoCloseable
{
    private final PageBuilder pageBuilder;
    private final ColumnSwitch<C> producer;
    private final List<TypeSinkCaller<C>> callers;

    public SwitchPageBuilder(BufferAllocator allocator, Schema schema, PageOutput output, ColumnSwitch<C> producer)
    {
        this.pageBuilder = new PageBuilder(allocator, schema, output);
        this.producer = producer;
        this.callers = ImmutableList.copyOf(Lists.transform(
                    schema.getColumns(), new Function<Column, TypeSinkCaller<C>>()
                    {
                        public TypeSinkCaller<C> apply(Column column)
                        {
                            return findSwitchCaseCaller(column, pageBuilder, (ColumnSwitch<C>) SwitchPageBuilder.this.producer);
                        }
                    }
                ));
    }

    public SwitchPageBuilder<C> apply(C context)
    {
        for (TypeSinkCaller<C> caller : callers) {
            caller.call(context);
        }
        return this;
    }

    public void addRecord()
    {
        pageBuilder.addRecord();
    }

    @Override
    public void close()
    {
        pageBuilder.close();
    }

    private static <C> TypeSinkCaller<C> findSwitchCaseCaller(Column column, PageBuilder pageBuilder, ColumnSwitch<C> valueProducer)
    {
        return column.getType().newTypeSinkCaller(valueProducer, pageBuilder, column);
    }
}
