package org.embulk.spi;

import org.embulk.config.CommitReport;

public interface TransactionalFileInput
        extends Transactional, FileInput
{
    public Buffer poll();

    public boolean nextFile();

    public void close();

    public void abort();

    public CommitReport commit();
}
