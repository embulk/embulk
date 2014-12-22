package org.embulk.spi;

import java.io.OutputStream;

public class FileOutputOutputStream
        extends OutputStream
{
    private final FileOutput out;
    private final BufferAllocator allocator;
    private int pos;
    private Buffer buffer;

    public FileOutputOutputStream(FileOutput out, BufferAllocator allocator)
    {
        this.out = out;
        this.allocator = allocator;
        this.buffer = allocator.allocate();
    }

    public void nextFile()
    {
        out.nextFile();
    }

    @Override
    public void write(int b)
    {
        buffer.array()[buffer.offset() + pos] = (byte) b;
        if (pos + 1 >= buffer.capacity()) {
            flush();
        } else {
            pos++;
        }
    }

    @Override
    public void write(byte[] b, int off, int len)
    {
        while (true) {
            int available = buffer.capacity() - pos;
            int wlen;
            if (available < len) {
                System.arraycopy(b, off, buffer.array(), buffer.offset() + pos, available);
                len -= available;
                off += available;
                flush();
            } else {
                System.arraycopy(b, off, buffer.array(), buffer.offset() + pos, len);
                if (available <= len) {
                    flush();
                }
                break;
            }
        }
    }

    @Override
    public void flush()
    {
        if (pos > 0) {
            buffer.limit(pos);
            out.add(buffer);
            buffer = allocator.allocate();
            pos = 0;
        }
    }

    public void finish()
    {
        flush();
        out.finish();
    }

    @Override
    public void close()
    {
        out.close();
    }
}
