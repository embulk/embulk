package org.embulk.config;

public interface TaskSource extends DataSource, org.embulk.api.v0.TaskSource {
    <T> T loadTask(Class<T> taskType);

    @Override
    TaskSource getNested(String attrName);

    @Override
    TaskSource getNestedOrSetEmpty(String attrName);

    @Override
    TaskSource getNestedOrGetEmpty(String attrName);

    @Override
    TaskSource set(String attrName, Object v);

    @Override
    TaskSource setNested(String attrName, DataSource v);

    @Override
    TaskSource setAll(DataSource other);

    @Override
    TaskSource remove(String attrName);

    @Override
    TaskSource deepCopy();

    @Override
    TaskSource merge(DataSource other);
}
