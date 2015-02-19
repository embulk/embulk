package org.embulk.spi.util;

import java.io.OutputStream;
import java.io.Closeable;
import java.io.IOException;
import org.embulk.spi.Buffer;
import org.embulk.spi.FileOutput;

public class OutputStreamFileOutput
        implements FileOutput
{
    public interface Provider extends Closeable
    {
        public OutputStream openNext() throws IOException;

        public void finish() throws IOException;

        public void close() throws IOException;
    }

    private final Provider provider;
    private OutputStream current;

    public OutputStreamFileOutput(Provider provider)
    {
        this.provider = provider;
        this.current = null;
    }

    public void nextFile()
    {
        closeCurrent();
        try {
            current = provider.openNext();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void add(Buffer buffer)
    {
        if (current == null) {
            throw new IllegalStateException("nextFile() must be called before poll()");
        }
        try {
            current.write(buffer.array(), buffer.offset(), buffer.limit());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            buffer.release();
        }
    }

    public void finish()
    {
        closeCurrent();
        try {
            provider.finish();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void close()
    {
        try {
            closeCurrent();
        } finally {
            try {
                provider.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void closeCurrent()
    {
        try {
            if (current != null) {
                current.close();
                current = null;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
