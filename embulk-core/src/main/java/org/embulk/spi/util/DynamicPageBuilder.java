package org.embulk.spi.util;

import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.embulk.spi.Schema;
import org.embulk.spi.Column;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.time.TimestampParser;

public class DynamicPageBuilder
{
    private final PageBuilder pageBuilder;
    private final Schema schema;
    private final List<DynamicColumnSetter> setters;
    private final Map<String, DynamicColumnSetter> columnLookup;

    public DynamicPageBuilder(BufferAllocator allocator, Schema schema, PageOutput output)
    {
        this.pageBuilder = new PageBuilder(allocator, schema, output);
        this.schema = schema;

        // TODO default value
        DynamicColumnSetterFactory factory = new DynamicColumnSetterFactory(
                DynamicColumnSetterFactory.nullDefaultValue());

        ImmutableList.Builder<DynamicColumnSetter> setters = ImmutableList.builder();
        ImmutableMap.Builder<String, DynamicColumnSetter> lookup = ImmutableMap.builder();
        for (Column c : schema.getColumns()) {
            DynamicColumnSetter setter = factory.newColumnSetter(pageBuilder, c);
            setters.add(setter);
            lookup.put(c.getName(), setter);
        }
        this.setters = setters.build();
        this.columnLookup = lookup.build();
    }

    public List<Column> getColumns()
    {
        return schema.getColumns();
    }

    public DynamicColumnSetter column(Column c)
    {
        return setters.get(c.getIndex());
    }

    public DynamicColumnSetter column(int index)
    {
        return setters.get(index);
    }

    public DynamicColumnSetter lookupColumn(String columnName)
    {
        DynamicColumnSetter setter = columnLookup.get(columnName);
        if (setter == null) {
            throw new DynamicColumnNotFoundException("Column '"+columnName+"' does not exist");
        }
        return setter;
    }

    public void addRecord()
    {
        pageBuilder.addRecord();
    }

    public void flush()
    {
        pageBuilder.flush();
    }

    public void finish()
    {
        pageBuilder.finish();
    }

    public void close()
    {
        pageBuilder.close();
    }
}
