package org.embulk.spi;

import org.embulk.config.CommitReport;

public interface Transactional
{
    public void abort();

    public CommitReport commit();
}
