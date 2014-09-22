package org.quickload.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Column
{
    private final int index;
    private final String name;
    private final Type type;

    @JsonCreator
    public Column(
            @JsonProperty("index") int index,
            @JsonProperty("name") String name,
            @JsonProperty("type") Type type)
    {
        this.index = index;
        this.name = name;
        this.type = type;
    }

    @JsonProperty("index")
    public int getIndex()
    {
        return index;
    }

    @JsonProperty("name")
    public String getName()
    {
        return name;
    }

    @JsonProperty("type")
    public Type getType()
    {
        return type;
    }

    public void consume(RecordCursor cursor, RecordConsumer consumer)
    {
        type.consume(cursor, consumer, this);
    }

    public void produce(RecordBuilder builder, RecordProducer producer)
    {
        type.produce(builder, producer, this);
    }
}
