package org.embulk.config;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataSource
{
    public List<String> getAttributeNames();

    public Iterable<Map.Entry<String, JsonNode>> getAttributes();

    public boolean isEmpty();

    public boolean has(String attrName);

    public <E> E get(Class<E> type, String attrName);

    public <E> E get(Class<E> type, String attrName, E defaultValue);

    public DataSource getNested(String attrName);

    public DataSource getNestedOrSetEmpty(String attrName);

    public DataSource set(String attrName, Object v);

    public DataSource setNested(String attrName, DataSource v);

    public DataSource setAll(DataSource other);

    public DataSource remove(String attrName);

    public DataSource deepCopy();

    public DataSource merge(DataSource other);

    public ObjectNode getObjectNode();
}
