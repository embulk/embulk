package org.embulk.config;

/**
 * Simply replaced by TaskReport.
 *
 * To be removed by v0.10 or earlier.
 */
@Deprecated  // https://github.com/embulk/embulk/issues/933
public interface CommitReport extends TaskReport {
    @Override
    CommitReport getNested(String attrName);

    @Override
    CommitReport getNestedOrSetEmpty(String attrName);

    @Override
    CommitReport getNestedOrGetEmpty(String attrName);

    @Override
    CommitReport set(String attrName, Object v);

    @Override
    CommitReport setNested(String attrName, DataSource v);

    @Override
    CommitReport setAll(DataSource other);

    @Override
    CommitReport remove(String attrName);

    @Override
    CommitReport deepCopy();

    @Override
    CommitReport merge(DataSource other);
}
