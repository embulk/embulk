package org.embulk.spi;

import org.embulk.config.CommitReport;

public interface TransactionalPageOutput
        extends Transactional, PageOutput
{
    public void add(Page page);

    public void finish();

    public void close();

    public void abort();

    public CommitReport commit();
}
