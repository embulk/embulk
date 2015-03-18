package org.embulk.spi.util;

import java.util.Arrays;
import java.util.Iterator;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Closeable;
import java.io.IOException;
import org.embulk.spi.Buffer;
import org.embulk.spi.FileInput;
import org.embulk.spi.BufferAllocator;

public class InputStreamFileInput
        implements FileInput
{
    public interface Provider extends Closeable
    {
        public InputStream openNext() throws IOException;

        public void close() throws IOException;
    }

    public static class IteratorProvider implements Provider {
        private Iterator<InputStream> iterator;

        public IteratorProvider(Iterable<InputStream> iterable)
        {
            this.iterator = iterable.iterator();
        }

        public IteratorProvider(Iterator<InputStream> iterator)
        {
            this.iterator = iterator;
        }

        public InputStream openNext() throws IOException
        {
            if (!iterator.hasNext()) {
                return null;
            }
            return iterator.next();
        }

        @Override
        public void close() throws IOException
        {
            while (iterator.hasNext()) {
                iterator.next().close();
            }
        }
    }

    private final BufferAllocator allocator;
    private final Provider provider;
    private InputStream current;

    public InputStreamFileInput(BufferAllocator allocator, Provider provider)
    {
        this.allocator = allocator;
        this.provider = provider;
        this.current = null;
    }

    public Buffer poll()
    {
        // TODO check current != null and throw Illegal State - file is not opened
        if (current == null) {
            throw new IllegalStateException("nextFile() must be called before poll()");
        }
        Buffer buffer = allocator.allocate();
        try {
            int n = current.read(buffer.array(), buffer.offset(), buffer.capacity());
            if (n < 0) {
                return null;
            }
            buffer.limit(n);
            Buffer b = buffer;
            buffer = null;
            return b;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (buffer != null) {
                buffer.release();
                buffer = null;
            }
        }
    }

    public boolean nextFile()
    {
        try {
            if (current != null) {
                current.close();
                current = null;
            }

            InputStream in = provider.openNext();
            if (in == null) {
                return false;
            }
            current = new BufferedInputStream(in);
            return true;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void close()
    {
        try {
            provider.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
