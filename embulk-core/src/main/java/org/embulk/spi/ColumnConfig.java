package org.embulk.spi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.embulk.config.ConfigSource;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.TimestampType;

public class ColumnConfig
{
    private final String name;
    private final Type type;
    private final ConfigSource options;

    @JsonCreator
    public ColumnConfig(ConfigSource config)
    {
        this.name = config.get(String.class, "name");
        this.type = config.get(Type.class, "type");
        this.options = config.deepCopy();
        this.options.remove("name");
        this.options.remove("type");
    }

    public ColumnConfig(String name, Type type,
            ConfigSource options)
    {
        this.name = name;
        this.type = type;
        this.options = options;
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

    @Deprecated
    public String getFormat()
    {
        return options.get(String.class, "format", null);
    }

    @JsonValue
    public ConfigSource getConfigSource()
    {
        ConfigSource config = options.deepCopy();
        config.set("name", name);
        config.set("type", type);
        return config;
    }

    public Column toColumn(int index)
    {
        String format = getFormat();
        if (type instanceof TimestampType && format != null) {
            return new Column(index, name, ((TimestampType) type).withFormat(format), options);
        } else {
            return new Column(index, name, type, options);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ColumnConfig)) {
            return false;
        }
        ColumnConfig other = (ColumnConfig) obj;
        return Objects.equals(this.name, other.name) &&
            Objects.equals(type, other.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, type);
    }

    @Override
    public String toString()
    {
        return String.format("ColumnConfig[%s, %s, %s]",
                getName(), getType(), options.toString());
    }
}
