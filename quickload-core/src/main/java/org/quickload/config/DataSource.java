package org.quickload.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

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

    // TODO getter methods, with default value and optional/requred flags

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
}
