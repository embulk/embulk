package org.embulk.spi;

import java.io.InputStream;

public class FileInputInputStream
        extends InputStream
{
    private final FileInput in;
    private int pos;
    private Buffer buffer = Buffer.EMPTY;

    public FileInputInputStream(FileInput in)
    {
        this.in = in;
    }

    public boolean markSupported()
    {
        return false;
    }

    public boolean nextFile()
    {
        releaseBuffer();
        return in.nextFile();
    }

    @Override
    public int available()
    {
        return buffer.limit() - pos;
    }

    @Override
    public int read()
    {
        while (pos >= buffer.limit()) {
            if (!nextBuffer()) {
                return -1;
            }
        }
        byte b = buffer.array()[buffer.offset() + pos];
        pos++;
        if (pos >= buffer.limit()) {
            releaseBuffer();
        }
        return b & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len)
    {
        while (pos >= buffer.limit()) {
            if (!nextBuffer()) {
                return -1;
            }
        }
        int remaining = buffer.limit() - pos;
        boolean allConsumed;
        if (remaining <= len) {
            allConsumed = true;
            len = remaining;
        } else {
            allConsumed = false;
        }
        if (b != null) {
            // b == null if skip
            buffer.getBytes(pos, b, off, len);
        }
        if (allConsumed) {
            releaseBuffer();
        } else {
            pos += len;
        }
        return len;
    }

    @Override
    public long skip(long len)
    {
        return read(null, 0, (int) Math.min(len, Integer.MAX_VALUE));
    }

    private boolean nextBuffer()
    {
        releaseBuffer();
        Buffer b = in.poll();
        if (b == null) {
            return false;
        }
        buffer = b;
        return true;
    }

    private void releaseBuffer()
    {
        buffer.release();
        buffer = Buffer.EMPTY;
        pos = 0;
    }

    @Override
    public void close()
    {
        releaseBuffer();
        in.close();
    }
}
