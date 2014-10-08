package org.quickload.spi;

public interface ParserPlugin<T extends ParserTask>
{
    public BufferOperator openOperator(T task, int processorIndex, OutputOperator op);

    public void shutdown();
}
