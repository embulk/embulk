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
    private final BufferInput input;

    public LineDecoder(BufferInput input, LineDecoderTask task)
    {
        this.input = input;
    }

    private class Ite
            implements Iterator<String>
    {
        private LinkedList<String> lines = new LinkedList();

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
            Buffer buffer = input.poll();
            if (buffer == null) {
                return false;
            }

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
        return new Ite();
    }
}

