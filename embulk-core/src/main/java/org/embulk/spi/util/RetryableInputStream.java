package org.embulk.spi.util;

import java.io.InputStream;
import java.io.IOException;

public class RetryableInputStream
        extends InputStream
{
    public interface Reopener
    {
        public InputStream reopen(long offset, Exception closedCause) throws IOException;
    }

    private final Reopener reopener;
    protected InputStream in;
    private long offset;
    private long markedOffset;

    public RetryableInputStream(InputStream initialInputStream, Reopener reopener)
    {
        this.reopener = reopener;
        this.in = initialInputStream;
        this.offset = 0L;
        this.markedOffset = 0L;
    }

    public RetryableInputStream(Reopener reopener) throws IOException
    {
        this(reopener.reopen(0, null), reopener);
    }

    private void reopen(Exception closedCause) throws IOException
    {
        if (in != null) {
            in.close();
            in = null;
        }
        in = reopener.reopen(offset, closedCause);
    }

    @Override
    public int read() throws IOException
    {
        while (true) {
            try {
                int v = in.read();
                offset += 1;
                return v;
            } catch (IOException | RuntimeException ex) {
                reopen(ex);
            }
        }
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        while (true) {
            try {
                int r = in.read(b);
                offset += r;
                return r;
            } catch (IOException | RuntimeException ex) {
                reopen(ex);
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        while (true) {
            try {
                int r = in.read(b, off, len);
                offset += r;
                return r;
            } catch (IOException | RuntimeException ex) {
                reopen(ex);
            }
        }
    }

    @Override
    public long skip(long n) throws IOException
    {
        while (true) {
            try {
                long r = in.skip(n);
                offset += r;
                return r;
            } catch (IOException | RuntimeException ex) {
                reopen(ex);
            }
        }
    }

    @Override
    public int available() throws IOException
    {
        return in.available();
    }

    @Override
    public void close() throws IOException
    {
        in.close();
    }

    @Override
    public void mark(int readlimit)
    {
        in.mark(readlimit);
        markedOffset = offset;
    }

    @Override
    public void reset() throws IOException
    {
        in.reset();
        offset = markedOffset;
    }

    @Override
    public boolean markSupported()
    {
        return in.markSupported();
    }
}
