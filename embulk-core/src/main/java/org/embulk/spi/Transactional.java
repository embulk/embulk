package org.embulk.spi;

import org.embulk.config.TaskReport;

public interface Transactional
{
    void abort();

    TaskReport commit();
}
