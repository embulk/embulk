package org.embulk.spi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.BooleanType;
import org.embulk.spi.type.DoubleType;
import org.embulk.spi.type.LongType;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.type.StringType;

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

    public void visit(ColumnVisitor visitor)
    {
        switch(type.getTypeEnum()) {
        case BOOLEAN:
            visitor.booleanColumn(this);
            break;
        case LONG:
            visitor.longColumn(this);
            break;
        case DOUBLE:
            visitor.doubleColumn(this);
            break;
        case STRING:
            visitor.stringColumn(this);
            break;
        case TIMESTAMP:
            visitor.timestampColumn(this);
            break;
        default:
            assert false : type;
        }
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
        return Objects.equals(index, other.index) &&
            Objects.equals(name, other.name) &&
            Objects.equals(type, other.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(index, name, type);
    }

    @Override
    public String toString()
    {
        return String.format("Column{index:%d, name:%s, type:%s}",
                getIndex(), getName(), getType().getName());
    }
}
