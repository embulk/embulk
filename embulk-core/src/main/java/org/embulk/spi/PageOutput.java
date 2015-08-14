package org.embulk.spi;

public interface PageOutput
        extends AutoCloseable
{
    void add(Page page);

    void finish();

    void close();
}
