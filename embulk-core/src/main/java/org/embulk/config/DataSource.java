package org.embulk.config;

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.io.IOException;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public abstract class DataSource <T extends DataSource>
{
    protected final ObjectNode data;
    protected final ModelManager model;

    public DataSource(ModelManager model)
    {
        this(model, new ObjectNode(JsonNodeFactory.instance));
    }

    protected DataSource(ModelManager model, ObjectNode data)
    {
        this.data = data;
        this.model = model;
    }

    protected abstract T newInstance(ModelManager model, ObjectNode data);

    /*
    public static ObjectNode parseJson(ModelManager model, JsonParser jsonParser) throws IOException
    {
        ObjectNode json = model.readObject(jsonParser, ObjectNode.class);
    }

    public static ObjectNode parseJson(ModelManager model, String jsonString) throws IOException
    {
        JsonNode json = new ObjectMapper().readTree(jsonString);
        if (!json.isObject()) {
            throw new JsonMappingException("Expected object to deserialize DataSource but got "+json);
        }
        return (ObjectNode) json;
    }
    */

    // visible for DataSourceSerDe.DataSourceSerializer
    ObjectNode getObjectNode()
    {
        return data;
    }

    public List<String> getAttributeNames()
    {
        return ImmutableList.copyOf(data.fieldNames());
    }

    public Iterable<Map.Entry<String, JsonNode>> getAttributes()
    {
        return new Iterable() {
            public Iterator<Map.Entry<String, JsonNode>> iterator()
            {
                return data.fields();
            }
        };
    }

    public <E> E get(Class<E> type, String attrName)
    {
        JsonNode json = data.get(attrName);
        if (json == null) {
            throw new ConfigException("Attribute "+attrName+" is required but not set");
        }
        return model.readObject(type, json.traverse());
    }

    public <E> E get(Class<E> type, String attrName, E defaultValue)
    {
        JsonNode json = data.get(attrName);
        if (json == null) {
            return defaultValue;
        }
        return model.readObject(type, json.traverse());
    }

    public T getNested(String attrName)
    {
        JsonNode json = data.get(attrName);
        if (json == null) {
            throw new ConfigException("Attribute "+attrName+" is required but not set");
        }
        if (!json.isObject()) {
            throw new ConfigException("Attribute "+attrName+" must be an object");
        }
        return newInstance(model, (ObjectNode) json);
    }

    public T getNestedOrSetEmpty(String attrName)
    {
        JsonNode json = data.get(attrName);
        if (json == null) {
            json = data.objectNode();
            data.set(attrName, json);
        } else if (!json.isObject()) {
            throw new ConfigException("Attribute "+attrName+" must be an object");
        }
        return newInstance(model, (ObjectNode) json);
    }

    public T set(String attrName, Object v)
    {
        if (v == null) {
            data.remove(attrName);
        } else {
            data.put(attrName, model.writeObjectAsJsonNode(v));
        }
        return (T) this;
    }

    public T setNested(String attrName, DataSource<?> v)
    {
        data.put(attrName, v.data);
        return (T) this;
    }

    public T setAll(DataSource<?> other)
    {
        Iterator<Map.Entry<String, JsonNode>> fields = other.data.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            data.put(field.getKey(), field.getValue());
        }
        return (T) this;
    }

    public T deepCopy()
    {
        return newInstance(model, data.deepCopy());
    }

    public T merge(DataSource<?> other)
    {
        mergeJsonObject(data, other.data.deepCopy());
        return (T) this;
    }

    private static void mergeJsonObject(ObjectNode src, ObjectNode other)
    {
        Iterator<Map.Entry<String, JsonNode>> ite = other.fields();
        while (ite.hasNext()) {
            Map.Entry<String, JsonNode> pair = ite.next();
            JsonNode s = src.get(pair.getKey());
            JsonNode v = pair.getValue();
            if (v.isObject() && s != null && s.isObject()) {
                mergeJsonObject((ObjectNode) s, (ObjectNode) v);
            } else if (v.isArray() && s != null && s.isArray()) {
                mergeJsonArray((ArrayNode) s, (ArrayNode) v);
            } else {
                src.replace(pair.getKey(), v);
            }
        }
    }

    private static void mergeJsonArray(ArrayNode src, ArrayNode other)
    {
        src.addAll(other);
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
