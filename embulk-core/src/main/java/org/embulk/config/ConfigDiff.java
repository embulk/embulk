package org.embulk.config;

public interface ConfigDiff
        extends DataSource
{
    @Override
    ConfigDiff getNested(String attrName);

    @Override
    ConfigDiff getNestedOrSetEmpty(String attrName);

    @Override
    ConfigDiff getNestedOrGetEmpty(String attrName);

    @Override
    ConfigDiff set(String attrName, Object v);

    @Override
    ConfigDiff setNested(String attrName, DataSource v);

    @Override
    ConfigDiff setAll(DataSource other);

    @Override
    ConfigDiff remove(String attrName);

    @Override
    ConfigDiff deepCopy();

    @Override
    ConfigDiff merge(DataSource other);
}
