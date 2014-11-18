package org.quickload.config;

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

    public DataSource()
    {
        this(new ObjectNode(JsonNodeFactory.instance));
    }

    protected DataSource(ObjectNode data)
    {
        this.data = data;
    }

    protected abstract T newInstance(ObjectNode data);

    public static ObjectNode parseJson(JsonParser jsonParser) throws IOException
    {
        JsonNode json = new ObjectMapper().readTree(jsonParser);
        if (!json.isObject()) {
            throw new JsonMappingException("Expected object to deserialize DataSource but got "+json);
        }
        return (ObjectNode) json;
    }

    public static ObjectNode parseJson(String jsonString) throws IOException
    {
        JsonNode json = new ObjectMapper().readTree(jsonString);
        if (!json.isObject()) {
            throw new JsonMappingException("Expected object to deserialize DataSource but got "+json);
        }
        return (ObjectNode) json;
    }

    /**
     * visible for DataSourceSerDe, ModelManager
     */
    ObjectNode getSource()
    {
        return data;
    }

    public List<String> getFieldNames()
    {
        return ImmutableList.copyOf(data.fieldNames());
    }

    public Iterable<Map.Entry<String, JsonNode>> getFields()
    {
        return new Iterable() {
            public Iterator<Map.Entry<String, JsonNode>> iterator()
            {
                return data.fields();
            }
        };
    }

    public JsonNode get(String fieldName)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            throw new ConfigException("Field "+fieldName+" is required but not set");
        }
        return json;
    }

    public JsonNode get(String fieldName, JsonNode defaultValue)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            return defaultValue;
        }
        return json;
    }

    public T getObject(String fieldName)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            throw new ConfigException("Field "+fieldName+" is required but not set");
        }
        if (!json.isObject()) {
            throw new ConfigException("Field "+fieldName+" must be an object");
        }
        return newInstance((ObjectNode) json);
    }

    public T getObjectOrSetEmpty(String fieldName)
    {
        JsonNode json = data.get(fieldName);
        if (json == null) {
            json = data.objectNode();
            data.set(fieldName, json);
        } else if (!json.isObject()) {
            throw new ConfigException("Field "+fieldName+" must be an object");
        }
        return newInstance((ObjectNode) json);
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
        data.replace(fieldName, v);
        return (T) this;
    }

    public T set(String fieldName, DataSource<?> v)
    {
        return set(fieldName, v.data);
    }

    public T setAll(ObjectNode object)
    {
        Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            data.put(field.getKey(), field.getValue());
        }
        return (T) this;
    }

    public T setAll(DataSource<?> other)
    {
        return setAll(other.data);
    }

    public T deepCopy()
    {
        return newInstance(data.deepCopy());
    }

    public T mergeRecursively(DataSource<?> other)
    {
        mergeJsonObject(data, other.data);
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

    public static ArrayNode arrayNode()
    {
        return JsonNodeFactory.instance.arrayNode();
    }

    public static ObjectNode objectNode()
    {
        return JsonNodeFactory.instance.objectNode();
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
