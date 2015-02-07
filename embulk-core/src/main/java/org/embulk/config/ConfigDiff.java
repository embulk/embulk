package org.embulk.config;

public interface ConfigDiff
        extends DataSource
{
    @Override
    public ConfigDiff getNested(String attrName);

    @Override
    public ConfigDiff getNestedOrSetEmpty(String attrName);

    @Override
    public ConfigDiff set(String attrName, Object v);

    @Override
    public ConfigDiff setNested(String attrName, DataSource v);

    @Override
    public ConfigDiff setAll(DataSource other);

    @Override
    public ConfigDiff deepCopy();

    @Override
    public ConfigDiff merge(DataSource other);
}
