package org.embulk.deps.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;

public class DataSourceImpl implements ConfigSource, TaskSource, TaskReport, ConfigDiff {
    protected final ObjectNode data;

    protected final ModelManagerDelegateImpl model;

    public DataSourceImpl(ModelManagerDelegateImpl model) {
        this(model, new ObjectNode(JsonNodeFactory.instance));
    }

    // visible for DataSourceSerDe, ConfigSourceLoader and TaskInvocationHandler.dump
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public DataSourceImpl(ModelManagerDelegateImpl model, ObjectNode data) {
        this.data = data;
        this.model = model;
    }

    protected DataSourceImpl newInstance(ModelManagerDelegateImpl model, ObjectNode data) {
        return new DataSourceImpl(model, (ObjectNode) data);
    }

    // It was overridden from DataSource, but getObjectNode is removed from DataSource.
    // It can be package-private soon. DataSourceSerDe.DataSourceSerializer is the only user in embulk-core.
    @Deprecated
    public ObjectNode getObjectNode() {
        return data;
    }

    @Override
    public List<String> getAttributeNames() {
        final ArrayList<String> copy = new ArrayList<>();
        data.fieldNames().forEachRemaining(copy::add);
        return Collections.unmodifiableList(copy);
    }

    // It was overridden from DataSource, but getAttributes is removed from DataSource.
    @Deprecated
    public Iterable<Map.Entry<String, JsonNode>> getAttributes() {
        return new Iterable<Map.Entry<String, JsonNode>>() {
            public Iterator<Map.Entry<String, JsonNode>> iterator() {
                return data.fields();
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return !data.fieldNames().hasNext();
    }

    @Override
    public boolean has(String attrName) {
        return data.has(attrName);
    }

    @Override
    public boolean hasList(final String attrName) {
        if (!this.data.has(attrName)) {
            return false;
        }

        final JsonNode json = this.data.get(attrName);
        if (json == null) {
            return false;
        }
        return json.isArray();
    }

    @Override
    public boolean hasNested(final String attrName) {
        if (!this.data.has(attrName)) {
            return false;
        }

        final JsonNode json = this.data.get(attrName);
        if (json == null) {
            return false;
        }
        return json.isObject();
    }

    @Override
    public <E> E get(Class<E> type, String attrName) {
        JsonNode json = data.get(attrName);
        if (json == null) {
            throw new ConfigException("Attribute " + attrName + " is required but not set");
        }
        return model.readObject(type, json.traverse());
    }

    @Override
    public <E> E get(Class<E> type, String attrName, E defaultValue) {
        JsonNode json = data.get(attrName);
        if (json == null) {
            return defaultValue;
        }
        return model.readObject(type, json.traverse());
    }

    @Override
    public <E> List<E> getListOf(final Class<E> type, final String attrName) {
        final JsonNode json = this.data.get(attrName);
        if (json == null) {
            return Collections.emptyList();
        }
        if (!json.isArray()) {
            throw new ConfigException("Attribute " + attrName + " must be an array");
        }
        final ArrayList<E> list = new ArrayList<>();
        for (final JsonNode element : (Iterable<JsonNode>) () -> json.elements()) {
            list.add(model.readObject(type, element.traverse()));
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public DataSourceImpl getNested(String attrName) {
        JsonNode json = data.get(attrName);
        if (json == null) {
            throw new ConfigException("Attribute " + attrName + " is required but not set");
        }
        if (!json.isObject()) {
            throw new ConfigException("Attribute " + attrName + " must be an object");
        }
        return newInstance(model, (ObjectNode) json);
    }

    @Override
    public DataSourceImpl getNestedOrSetEmpty(String attrName) {
        JsonNode json = data.get(attrName);
        if (json == null) {
            json = data.objectNode();
            data.set(attrName, json);
        } else if (!json.isObject()) {
            throw new ConfigException("Attribute " + attrName + " must be an object");
        }
        return newInstance(model, (ObjectNode) json);
    }

    @Override
    public DataSourceImpl getNestedOrGetEmpty(String attrName) {
        JsonNode json = data.get(attrName);
        if (json == null) {
            json = data.objectNode();
        } else if (!json.isObject()) {
            throw new ConfigException("Attribute " + attrName + " must be an object");
        }
        return newInstance(model, (ObjectNode) json);
    }

    @Override
    public DataSourceImpl set(String attrName, Object v) {
        if (v == null) {
            remove(attrName);
        } else {
            data.set(attrName, model.writeObjectAsJsonNode(v));
        }
        return this;
    }

    @Override
    public DataSourceImpl setNested(String attrName, DataSource v) {
        if (v == null) {
            this.data.set(attrName, null);
        } else {
            final String vJsonStringified = v.toJson();
            if (vJsonStringified == null) {
                throw new ConfigException(new NullPointerException("DataSource#setNested accepts only valid DataSource"));
            }
            final JsonNode vJsonNode = this.model.readObject(JsonNode.class, vJsonStringified);
            if (!vJsonNode.isObject()) {
                throw new ConfigException(new ClassCastException("DataSource#setNested accepts only valid JSON object"));
            }
            this.data.set(attrName, (ObjectNode) vJsonNode);
        }
        return this;
    }

    @Override
    public DataSourceImpl setAll(DataSource other) {
        if (other == null) {
            throw new ConfigException(new NullPointerException("DataSource#setAll accepts only non-null value"));
        }
        final String otherJsonStringified = other.toJson();
        if (otherJsonStringified == null) {
            throw new ConfigException(new NullPointerException("DataSource#setAll accepts only valid DataSource"));
        }
        final JsonNode otherJsonNode = this.model.readObject(JsonNode.class, otherJsonStringified);
        if (!otherJsonNode.isObject()) {
            throw new ConfigException(new ClassCastException("DataSource#setAll accepts only valid JSON object"));
        }
        final ObjectNode otherObjectNode = (ObjectNode) otherJsonNode;
        for (Map.Entry<String, JsonNode> field : (Iterable<Map.Entry<String, JsonNode>>) () -> otherObjectNode.fields()) {
            this.data.set(field.getKey(), field.getValue());
        }
        return this;
    }

    @Override
    public DataSourceImpl remove(String attrName) {
        data.remove(attrName);
        return this;
    }

    @Override
    public DataSourceImpl deepCopy() {
        return newInstance(model, data.deepCopy());
    }

    @Override
    public DataSourceImpl merge(DataSource other) {
        if (other == null) {
            throw new ConfigException(new NullPointerException("DataSource#merge accepts only non-null value"));
        }
        final String otherJsonStringified = other.toJson();
        if (otherJsonStringified == null) {
            throw new ConfigException(new NullPointerException("DataSource#merge accepts only valid DataSource"));
        }
        final JsonNode otherJsonNode = this.model.readObject(JsonNode.class, otherJsonStringified);
        if (!otherJsonNode.isObject()) {
            throw new ConfigException(new ClassCastException("DataSource#setAll accepts only valid JSON object"));
        }
        mergeJsonObject(data, (ObjectNode) otherJsonNode);
        return this;
    }

    @Override
    public String toJson() {
        return this.model.writeObject(this.data);
    }

    @Override
    public Map<String, Object> toMap() {
        return jsonObjectToMap(this.data);
    }

    private static void mergeJsonObject(ObjectNode src, ObjectNode other) {
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

    private static void mergeJsonArray(ArrayNode src, ArrayNode other) {
        for (int i = 0; i < other.size(); i++) {
            JsonNode s = src.get(i);
            JsonNode v = other.get(i);
            if (s == null) {
                src.add(v);
            } else if (v.isObject() && s.isObject()) {
                mergeJsonObject((ObjectNode) s, (ObjectNode) v);
            } else if (v.isArray() && s.isArray()) {
                mergeJsonArray((ArrayNode) s, (ArrayNode) v);
            } else {
                src.remove(i);
                src.insert(i, v);
            }
        }
    }

    private static Map<String, Object> jsonObjectToMap(final ObjectNode object) {
        final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonNode> field : (Iterable<Map.Entry<String, JsonNode>>) () -> object.fields()) {
            map.put(field.getKey(), jsonToPlain(field.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }

    private static List<Object> jsonArrayToList(final ArrayNode array) {
        final ArrayList<Object> list = new ArrayList<>();
        for (final JsonNode element : (Iterable<JsonNode>) () -> array.elements()) {
            list.add(jsonToPlain(element));
        }
        return Collections.unmodifiableList(list);
    }

    private static Object jsonToPlain(final JsonNode json) {
        if (json == null || json.isNull()) {
            return null;
        } else if (json.isBoolean()) {
            return json.booleanValue();
        } else if (json.isInt()) {
            return json.intValue();
        } else if (json.isLong()) {
            return json.longValue();
        } else if (json.isDouble()) {
            return json.doubleValue();
        } else if (json.isTextual()) {
            return json.textValue();
        } else if (json.isArray()) {
            return jsonArrayToList((ArrayNode) json);
        } else if (json.isObject()) {
            return jsonObjectToMap((ObjectNode) json);
        } else {
            throw new ConfigException("Unexpected JSON node type: " + json.getNodeType().toString());
        }
    }

    @Override
    @Deprecated
    public <T> T loadTask(Class<T> taskType) {
        return model.readObject(taskType, data.traverse());
    }

    @Override
    @Deprecated
    public <T> T loadConfig(Class<T> taskType) {
        return model.readObjectWithConfigSerDe(taskType, data.traverse());
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof DataSource)) {
            return false;
        }
        final DataSource otherDataSource = (DataSource) other;
        final String otherJsonStringified = otherDataSource.toJson();
        if (otherJsonStringified == null) {
            return false;
        }
        final JsonNode otherJsonNode = this.model.readObject(JsonNode.class, otherJsonStringified);
        if (!otherJsonNode.isObject()) {
            return false;
        }
        return data.equals((ObjectNode) otherJsonNode);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
