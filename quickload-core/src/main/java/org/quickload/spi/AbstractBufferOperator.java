package org.quickload.spi;

import org.quickload.buffer.Buffer;

public abstract class AbstractBufferOperator
        implements BufferOperator {

    public AbstractBufferOperator() {
    }

    @Override
    public abstract void addBuffer(Buffer buffer);

    @Override
    public Report failed(Exception cause) {
        return new FailedReport(null, null);
    }

    @Override
    public abstract Report completed();

    @Override
    public abstract void close() throws Exception;

}
