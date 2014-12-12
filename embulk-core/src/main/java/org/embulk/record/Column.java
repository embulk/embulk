package org.embulk.record;

import com.google.common.base.Objects;
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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Column)) {
            return false;
        }
        Column other = (Column) obj;
        return Objects.equal(index, other.index) &&
            Objects.equal(name, other.name) &&
            Objects.equal(type, other.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(index, name, type);
    }

    @Override
    public String toString()
    {
        return String.format("Column{index:%d, name:%s, type:%s}",
                getIndex(), getName(), getType().getName());
    }
}
