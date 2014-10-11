package org.quickload.standards;

import org.quickload.buffer.Buffer;
import org.quickload.spi.AbstractBufferOperator;
import org.quickload.spi.Report;

public class TreasureDataOutputOperator
        extends AbstractBufferOperator
{
    @Override
    public void addBuffer(Buffer buffer) {

    }

    @Override
    public Report completed() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
