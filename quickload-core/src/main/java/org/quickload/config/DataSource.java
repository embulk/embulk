package org.quickload.config;

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class DataSource <T extends DataSource>
{
    protected final ObjectNode data;
    protected final FieldMapper fieldMapper;

    public DataSource()
    {
        this(new ObjectNode(JsonNodeFactory.instance));
    }

    public DataSource(ObjectNode data)
    {
        this(data, null);
    }

    protected DataSource(FieldMapper fieldMapper)
    {
        this(new ObjectNode(JsonNodeFactory.instance), fieldMapper);
    }

    protected DataSource(ObjectNode data, FieldMapper fieldMapper)
    {
        this.data = data;
        this.fieldMapper = fieldMapper;
    }

    public <T extends Task> T loadModel(ModelManager modelManager, Class<T> iface)
    {
        if (fieldMapper == null) {
            return modelManager.readTask(data, iface);
        } else {
            return modelManager.readTask(data, iface, fieldMapper);
        }
    }

    /**
     * visible for DataSourceSerDe
     */
    ObjectNode getSource()
    {
        return data;
    }

    public List<String> getFieldNames()
    {
        return ImmutableList.copyOf(data.fieldNames());
    }

    public boolean getBoolean(String fieldName)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            throw new ConfigException("Field "+fieldName+" is required but not set");
        }
        if (!json.isBoolean()) {
            throw new ConfigException("Field "+fieldName+" must be a boolean");
        }
        return json.asBoolean();
    }

    public boolean getBoolean(String fieldName, boolean defaultValue)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            return defaultValue;
        }
        if (!json.canConvertToInt()) {
            throw new ConfigException("Field "+fieldName+" must be a boolean");
        }
        return json.asBoolean();
    }

    public int getInt(String fieldName)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            throw new ConfigException("Field "+fieldName+" is required but not set");
        }
        if (!json.canConvertToInt()) {
            throw new ConfigException("Field "+fieldName+" must be an integer and within 32-bit signed int");
        }
        return json.asInt();
    }

    public int getInt(String fieldName, int defaultValue)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            return defaultValue;
        }
        if (!json.canConvertToInt()) {
            throw new ConfigException("Field "+fieldName+" must be an integer and within 32-bit signed int");
        }
        return json.asInt();
    }

    public long getLong(String fieldName)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            throw new ConfigException("Field "+fieldName+" is required but not set");
        }
        if (!json.canConvertToLong()) {
            throw new ConfigException("Field "+fieldName+" must be an integer and within 64-bit signed int");
        }
        return json.asLong();
    }

    public long getLong(String fieldName, long defaultValue)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            return defaultValue;
        }
        if (!json.canConvertToLong()) {
            throw new ConfigException("Field "+fieldName+" must be an integer and within 64-bit signed int");
        }
        return json.asLong();
    }

    public double getDouble(String fieldName)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            throw new ConfigException("Field "+fieldName+" is required but not set");
        }
        if (!json.isDouble()) {
            throw new ConfigException("Field "+fieldName+" must be double");
        }
        return json.asDouble();
    }

    public double getDouble(String fieldName, double defaultValue)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            return defaultValue;
        }
        if (!json.isDouble()) {
            throw new ConfigException("Field "+fieldName+" must be double");
        }
        return json.asDouble();
    }

    public String getString(String fieldName)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            throw new ConfigException("Field "+fieldName+" is required but not set");
        }
        if (!json.isTextual()) {
            throw new ConfigException("Field "+fieldName+" must be a string");
        }
        return json.asText();
    }

    public String getString(String fieldName, String defaultValue)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            return defaultValue;
        }
        if (!json.isTextual()) {
            throw new ConfigException("Field "+fieldName+" must be a string");
        }
        return json.asText();
    }

    public T setBoolean(String fieldName, boolean v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T setInt(String fieldName, int v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T setLong(String fieldName, long v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T setDouble(String fieldName, double v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T setString(String fieldName, String v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T set(String fieldName, JsonNode v)
    {
        data.set(fieldName, v);
        return (T) this;
    }

    public T setAll(DataSource other)
    {
        Iterator<Map.Entry<String, JsonNode>> fields = other.data.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            data.put(field.getKey(), field.getValue());
        }
        return (T) this;
    }

    @Override
    public String toString()
    {
        return data.toString();
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof DataSource)) {
            return false;
        }
        return data.equals(((DataSource) other).data);
    }

    @Override
    public int hashCode()
    {
        return data.hashCode();
    }
}
