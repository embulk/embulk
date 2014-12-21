package org.embulk.spi;

public interface PageOutput
        extends AutoCloseable
{
    public void add(Page page);

    public void finish();

    public void close();
}
