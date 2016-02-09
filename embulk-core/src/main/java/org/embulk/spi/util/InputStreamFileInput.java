package org.embulk.spi.util;

import java.util.Arrays;
import java.util.Iterator;
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

    public interface Opener
    {
        public InputStream open() throws IOException;
    }

    public static class IteratorProvider implements Provider
    {
        private Iterator<InputStream> iterator;

        public IteratorProvider(Iterable<InputStream> iterable)
        {
            this.iterator = iterable.iterator();
        }

        public IteratorProvider(Iterator<InputStream> iterator)
        {
            this.iterator = iterator;
        }

        @Override
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

    private static class OpenerProvider implements Provider
    {
        private Opener opener;

        public OpenerProvider(Opener opener)
        {
            this.opener = opener;
        }

        @Override
        public InputStream openNext() throws IOException
        {
            if (opener == null) {
                return null;
            }
            InputStream stream = opener.open();
            opener = null;
            return stream;
        }

        @Override
        public void close() throws IOException
        { }
    }

    private static class InputStreamProvider implements Provider
    {
        private InputStream input;

        public InputStreamProvider(InputStream input)
        {
            this.input = input;
        }

        @Override
        public InputStream openNext() throws IOException
        {
            if (input == null) {
                return null;
            }
            InputStream ret = input;
            input = null;
            return ret;
        }

        @Override
        public void close() throws IOException
        {
            if (input != null) {
                input.close();
                input = null;
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

    public InputStreamFileInput(BufferAllocator allocator, Opener opener)
    {
        this(allocator, new OpenerProvider(opener));
    }

    public InputStreamFileInput(BufferAllocator allocator, InputStream openedStream)
    {
        this(allocator, new InputStreamProvider(openedStream));
    }

    public Buffer poll()
    {
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
            current = provider.openNext();
            return current != null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void close()
    {
        try {
            try {
                if (current != null) {
                    current.close();
                    current = null;
                }
            } finally {
                provider.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
