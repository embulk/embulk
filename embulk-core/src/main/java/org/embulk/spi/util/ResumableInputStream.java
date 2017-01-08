package org.embulk.spi.util;

import java.io.InputStream;
import java.io.IOException;

public class ResumableInputStream
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
    private Exception lastClosedCause;
    private boolean closed;

    public ResumableInputStream(InputStream initialInputStream, Reopener reopener)
    {
        this.reopener = reopener;
        this.in = initialInputStream;
        this.offset = 0L;
        this.markedOffset = 0L;
        this.lastClosedCause = null;
    }

    public ResumableInputStream(Reopener reopener) throws IOException
    {
        this(reopener.reopen(0, null), reopener);
    }

    private void reopen(Exception closedCause) throws IOException
    {
        if (in != null) {
            lastClosedCause = closedCause;
            try {
                in.close();
            } catch (IOException ignored) {
            }
            in = null;
        }
        in = reopener.reopen(offset, closedCause);
        lastClosedCause = null;
    }

    @Override
    public int read() throws IOException
    {
        ensureOpened();
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
        ensureOpened();
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
        ensureOpened();
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
        ensureOpened();
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
        ensureOpened();
        return in.available();
    }

    @Override
    public void close() throws IOException
    {
        if (in != null) {
            in.close();
            closed = true;
            in = null;
        }
    }

    @Override
    public void mark(int readlimit)
    {
        try {
            ensureOpened();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        in.mark(readlimit);
        markedOffset = offset;
    }

    @Override
    public void reset() throws IOException
    {
        ensureOpened();
        in.reset();
        offset = markedOffset;
    }

    @Override
    public boolean markSupported()
    {
        try {
            ensureOpened();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return in.markSupported();
    }

    private void ensureOpened() throws IOException
    {
        if (in == null) {
            if (closed) {
                throw new IOException("stream closed");
            }
            reopen(lastClosedCause);
        }
    }
}
