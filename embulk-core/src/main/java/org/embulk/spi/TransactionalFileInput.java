package org.embulk.spi;

import org.embulk.config.TaskReport;

public interface TransactionalFileInput
        extends Transactional, FileInput
{
    Buffer poll();

    boolean nextFile();

    void close();

    void abort();

    TaskReport commit();
}
