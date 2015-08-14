package org.embulk.spi;

import org.embulk.config.TaskReport;

public interface TransactionalFileOutput
        extends Transactional, FileOutput
{
    void nextFile();

    void add(Buffer buffer);

    void finish();

    void close();

    void abort();

    TaskReport commit();
}
