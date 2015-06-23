package org.embulk.spi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.embulk.config.ConfigSource;
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
    private final ConfigSource options;

    @JsonCreator
    public Column(ConfigSource config)
    {
        this.index = config.get(int.class, "index");
        this.name = config.get(String.class, "name");
        this.type = config.get(Type.class, "type");
        this.options = config.deepCopy();
        this.options.remove("index");
        this.options.remove("name");
        this.options.remove("type");
    }

    public Column(int index, String name, Type type,
            ConfigSource options)
    {
        this.index = index;
        this.name = name;
        this.type = type;
        this.options = options;
    }

    public int getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public Type getType()
    {
        return type;
    }

    public ConfigSource getOptions()
    {
        return options;
    }

    @JsonValue
    public ConfigSource getConfigSource()
    {
        ConfigSource config = options.deepCopy();
        config.set("index", index);
        config.set("name", name);
        config.set("type", type);
        return config;
    }

    public void visit(ColumnVisitor visitor)
    {
        if (type instanceof BooleanType) {
            visitor.booleanColumn(this);
        } else if (type instanceof LongType) {
            visitor.longColumn(this);
        } else if (type instanceof DoubleType) {
            visitor.doubleColumn(this);
        } else if (type instanceof StringType) {
            visitor.stringColumn(this);
        } else if (type instanceof TimestampType) {
            visitor.timestampColumn(this);
        } else {
            assert(false);
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
        return String.format("Column{index:%d, name:%s, type:%s, options:%s}",
                getIndex(), getName(), getType().getName(), options.toString());
    }
}
