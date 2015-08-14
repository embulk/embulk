package org.embulk.spi;

import org.embulk.config.CommitReport;

public interface Transactional
{
    void abort();

    CommitReport commit();
}
