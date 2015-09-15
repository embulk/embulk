package org.embulk.config;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DataSource
{
    List<String> getAttributeNames();

    Iterable<Map.Entry<String, JsonNode>> getAttributes();

    boolean isEmpty();

    boolean has(String attrName);

    <E> E get(Class<E> type, String attrName);

    <E> E get(Class<E> type, String attrName, E defaultValue);

    DataSource getNested(String attrName);

    DataSource getNestedOrSetEmpty(String attrName);

    DataSource getNestedOrGetEmpty(String attrName);

    DataSource set(String attrName, Object v);

    DataSource setNested(String attrName, DataSource v);

    DataSource setAll(DataSource other);

    DataSource remove(String attrName);

    DataSource deepCopy();

    DataSource merge(DataSource other);

    ObjectNode getObjectNode();
}
