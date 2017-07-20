package org.embulk.spi;

public interface Reporter
        extends AutoCloseable
{
    void skip(String skipped); // TODO should be changed
    //void skip(Buffer errorBufferData);
    //void skip(Record errorPageData);

    void close(); // TODO should consider about the return type

    void cleanup(); // TODO should return TaskReport??
}
