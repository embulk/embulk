package org.embulk.config;

public interface ConfigSource
        extends DataSource
{
    public <T> T loadConfig(Class<T> taskType);

    @Override
    public ConfigSource getNested(String attrName);

    @Override
    public ConfigSource getNestedOrSetEmpty(String attrName);

    @Override
    public ConfigSource set(String attrName, Object v);

    @Override
    public ConfigSource setNested(String attrName, DataSource v);

    @Override
    public ConfigSource setAll(DataSource other);

    @Override
    public ConfigSource remove(String attrName);

    @Override
    public ConfigSource deepCopy();

    @Override
    public ConfigSource merge(DataSource other);
}
