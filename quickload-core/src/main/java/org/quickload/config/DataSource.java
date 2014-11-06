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

    // TODO getter methods, with default value and optional/requred flags

    public List<String> getFieldNames()
    {
        return ImmutableList.copyOf(data.fieldNames());
    }

    public T putBoolean(String fieldName, boolean v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T putByteArray(String fieldName, byte[] v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T putDouble(String fieldName, double v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T putInt(String fieldName, int v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T putLong(String fieldName, long v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T putString(String fieldName, String v)
    {
        data.put(fieldName, v);
        return (T) this;
    }

    public T put(String fieldName, JsonNode v)
    {
        data.set(fieldName, v);
        return (T) this;
    }

    public T putAll(DataSource other)
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
