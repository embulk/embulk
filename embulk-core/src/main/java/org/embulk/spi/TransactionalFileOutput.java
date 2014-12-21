package org.embulk.spi;

import org.embulk.config.CommitReport;

public interface TransactionalFileOutput
        extends Transactional, FileOutput
{
    public void add(Buffer buffer);

    public void nextFile();

    public void finish();

    public void close();

    public void abort();

    public CommitReport commit();
}
