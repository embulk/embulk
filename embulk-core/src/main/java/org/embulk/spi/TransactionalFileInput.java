package org.embulk.spi;

import org.embulk.config.CommitReport;

public interface TransactionalFileInput
        extends Transactional, FileInput
{
    Buffer poll();

    boolean nextFile();

    void close();

    void abort();

    CommitReport commit();
}
