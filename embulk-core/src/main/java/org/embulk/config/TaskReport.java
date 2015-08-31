package org.embulk.config;

public interface TaskReport
        extends DataSource
{
    @Override
    TaskReport getNested(String attrName);

    @Override
    TaskReport getNestedOrSetEmpty(String attrName);

    @Override
    TaskReport getNestedOrGetEmpty(String attrName);

    @Override
    TaskReport set(String attrName, Object v);

    @Override
    TaskReport setNested(String attrName, DataSource v);

    @Override
    TaskReport setAll(DataSource other);

    @Override
    TaskReport remove(String attrName);

    @Override
    TaskReport deepCopy();

    @Override
    TaskReport merge(DataSource other);
}
