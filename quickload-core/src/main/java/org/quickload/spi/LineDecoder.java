package org.quickload.spi;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import org.quickload.buffer.Buffer;
import org.quickload.channel.BufferInput;

public class LineDecoder
        implements Iterable<String>
{
    private final Iterable<Buffer> input;

    public LineDecoder(Iterable<Buffer> input, LineDecoderTask task)
    {
        this.input = input;
    }

    private static class Ite
            implements Iterator<String>
    {
        private Iterator<Buffer> iterator;
        private LinkedList<String> lines = new LinkedList();

        public Ite(Iterator<Buffer> iterator)
        {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext()
        {
            while (lines.isEmpty()) {
                if (!readLines()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return lines.remove();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private boolean readLines()
        {
            if (!iterator.hasNext()) {
                return false;
            }
            Buffer buffer = iterator.next();

            // TODO needs internal buffer
            StringBuilder sbuf = new StringBuilder();
            // TODO use streaming decoder
            Charset charset = Charset.forName("UTF-8");
            CharBuffer cb = charset.decode(buffer.getBuffer());

            for (int i = 0; i < cb.capacity(); i++) {
                if (cb.get(i) != '\n') {
                    sbuf.append(cb.get(i));
                } else {
                    lines.add(sbuf.toString());
                    sbuf = new StringBuilder();
                }
            }

            return true;
        }
    }

    public Iterator<String> iterator()
    {
        return new Ite(input.iterator());
    }
}

