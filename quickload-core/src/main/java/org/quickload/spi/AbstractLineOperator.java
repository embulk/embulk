package org.quickload.spi;

import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.exec.BufferManager;
import org.quickload.record.PageBuilder;
import org.quickload.record.PageBuilder;
import org.quickload.record.Schema;

public abstract class AbstractLineOperator<T extends ParserTask>
        extends AbstractBufferOperator implements LineOperator
{
    protected final Schema schema; // TODO type parameter is needed?
    private final int processorIndex;
    private final OutputOperator op;

    protected final BufferManager bufferManager;
    protected final PageBuilder pageBuilder;

    @Inject
    public AbstractLineOperator(Schema schema, int processorIndex, OutputOperator op,
                                BufferManager bufferManager)
    {
        this.schema = schema;
        this.processorIndex = processorIndex;
        this.op = op;
        this.bufferManager = bufferManager;
        this.pageBuilder = new PageBuilder(bufferManager, schema, op);
    }

    @Override
    public void addBuffer(Buffer buffer)
    {
        // TODO simple parser and line operator
        byte[] bytes = buffer.get();
        StringBuilder sbuf = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            char c = (char)bytes[i];
            if (c == '\n' || c == '\r') {
                // TODO how can i create and use LineOperator??
                addLine(sbuf.toString());
                sbuf = new StringBuilder();
            } else {
                sbuf.append(c);
            }
        }

        if (sbuf.length() != 0) {
            addLine(sbuf.toString());
        }
    }

    @Override
    public abstract void addLine(String line);

    @Override
    public Report failed(Exception cause)
    {
        return op.failed(cause);
    }

    @Override
    public Report completed()
    {
        pageBuilder.flush();
        return op.completed();
    }

    @Override
    public void close() throws Exception {
        // TODO
    }
}
