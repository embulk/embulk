package org.embulk.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DataSourceImpl implements ConfigSource, TaskSource, TaskReport, ConfigDiff {
    protected final ObjectNode data;

    @Deprecated  // https://github.com/embulk/embulk/issues/1304
    protected final ModelManager model;

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public DataSourceImpl(ModelManager model) {
        this(model, new ObjectNode(JsonNodeFactory.instance));
    }

    // visible for DataSourceSerDe, ConfigSourceLoader and TaskInvocationHandler.dump
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public DataSourceImpl(ModelManager model, ObjectNode data) {
        this.data = data;
        this.model = model;
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    protected DataSourceImpl newInstance(ModelManager model, ObjectNode data) {
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
