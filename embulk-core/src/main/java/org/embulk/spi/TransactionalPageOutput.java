package org.embulk.spi;

import org.embulk.config.CommitReport;

public interface TransactionalPageOutput
        extends Transactional, PageOutput
{
    void add(Page page);

    void finish();

    void close();

    void abort();

    CommitReport commit();
}
