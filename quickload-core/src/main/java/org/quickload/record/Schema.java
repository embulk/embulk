package org.quickload.record;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Schema
{
    private final List<Column> columns;

    @JsonCreator
    public Schema(
            @JsonProperty("columns") List<Column> columns)
    {
        this.columns = columns;
    }

    @JsonProperty("columns")
    public List<Column> getColumns()
    {
        return columns;
    }

    public Column getColumn(int index)
    {
        return columns.get(index);
    }

    public String getColumnName(int index)
    {
        return getColumn(index).getName();
    }

    public Type getColumnType(int index)
    {
        return getColumn(index).getType();
    }

    public boolean isEmpty()
    {
        return columns.isEmpty();
    }

    public int size()
    {
        return columns.size();
    }

    public int getFixedStorageSize()
    {
        int total = 0;
        for (Column column : columns) {
            total += column.getType().getFixedStorageSize();
        }
        return total;
    }

    public void consume(RecordCursor cursor, RecordConsumer consumer)
    {
        for(Column c : columns) {
            c.consume(cursor, consumer);
        }
    }

    public void produce(RecordBuilder builder, RecordProducer producer)
    {
        for(Column c : columns) {
            c.produce(builder, producer);
        }
    }
}
