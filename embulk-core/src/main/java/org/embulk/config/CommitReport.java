package org.embulk.config;

public interface CommitReport
        extends DataSource
{
    @Override
    public CommitReport getNested(String attrName);

    @Override
    public CommitReport getNestedOrSetEmpty(String attrName);

    @Override
    public CommitReport set(String attrName, Object v);

    @Override
    public CommitReport setNested(String attrName, DataSource v);

    @Override
    public CommitReport setAll(DataSource other);

    @Override
    public CommitReport deepCopy();

    @Override
    public CommitReport merge(DataSource other);
}
