package org.embulk.spi.util;

import java.io.OutputStream;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.FileOutput;

public class FileOutputOutputStream
        extends OutputStream
{
    private final FileOutput out;
    private final BufferAllocator allocator;
    private final CloseMode closeMode;
    private int pos;
    private Buffer buffer;

    public static enum CloseMode {
        FLUSH,
        FLUSH_FINISH,
        FLUSH_FINISH_CLOSE,
        CLOSE;
    }

    public FileOutputOutputStream(FileOutput out, BufferAllocator allocator, CloseMode closeMode)
    {
        this.out = out;
        this.allocator = allocator;
        this.buffer = allocator.allocate();
        this.closeMode = closeMode;
    }

    public void nextFile()
    {
        out.nextFile();
    }

    public void finish()
    {
        doFlush();
        out.finish();
    }

    @Override
    public void write(int b)
    {
        buffer.array()[buffer.offset() + pos] = (byte) b;
        pos++;
        if (pos >= buffer.capacity()) {
            flush();
        }
    }

    @Override
    public void write(byte[] b, int off, int len)
    {
        while (true) {
            int available = buffer.capacity() - pos;
            if (available < len) {
                buffer.setBytes(pos, b, off, available);
                pos += available;
                len -= available;
                off += available;
                flush();
            } else {
                buffer.setBytes(pos, b, off, len);
                pos += len;
                if (available <= len) {
                    flush();
                }
                break;
            }
        }
    }

    private boolean doFlush()
    {
        if (pos > 0) {
            buffer.limit(pos);
            out.add(buffer);
            buffer = Buffer.EMPTY;
            pos = 0;
            return true;
        }
        return false;
    }

    @Override
    public void flush()
    {
        if (doFlush()) {
            buffer = allocator.allocate();
        }
    }

    @Override
    public void close()
    {
        switch (closeMode) {
        case FLUSH:
            doFlush();
            break;
        case FLUSH_FINISH:
            doFlush();
            out.finish();
            break;
        case FLUSH_FINISH_CLOSE:
            doFlush();
            out.finish();
            out.close();
            break;
        case CLOSE:
            out.close();
            break;
        }
        buffer.release();
        buffer = Buffer.EMPTY;
        pos = 0;
    }
}
