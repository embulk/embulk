package org.embulk.config;

public interface ConfigSource
        extends DataSource
{
    <T> T loadConfig(Class<T> taskType);

    @Override
    ConfigSource getNested(String attrName);

    @Override
    ConfigSource getNestedOrSetEmpty(String attrName);

    @Override
    ConfigSource getNestedOrGetEmpty(String attrName);

    @Override
    ConfigSource set(String attrName, Object v);

    @Override
    ConfigSource setNested(String attrName, DataSource v);

    @Override
    ConfigSource setAll(DataSource other);

    @Override
    ConfigSource remove(String attrName);

    @Override
    ConfigSource deepCopy();

    @Override
    ConfigSource merge(DataSource other);
}
