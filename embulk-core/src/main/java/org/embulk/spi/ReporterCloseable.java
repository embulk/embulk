package org.embulk.spi;

public interface ReporterCloseable
        extends AutoCloseable, Reporter
{
    void close(); // TODO should consider about the return type

    void cleanup(); // TODO should return TaskReport??
}
