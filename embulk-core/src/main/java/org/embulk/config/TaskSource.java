package org.embulk.config;

public interface TaskSource
        extends DataSource
{
    public <T> T loadTask(Class<T> taskType);

    @Override
    public TaskSource getNested(String attrName);

    @Override
    public TaskSource getNestedOrSetEmpty(String attrName);

    @Override
    public TaskSource set(String attrName, Object v);

    @Override
    public TaskSource setNested(String attrName, DataSource v);

    @Override
    public TaskSource setAll(DataSource other);

    @Override
    public TaskSource deepCopy();

    @Override
    public TaskSource merge(DataSource other);
}
