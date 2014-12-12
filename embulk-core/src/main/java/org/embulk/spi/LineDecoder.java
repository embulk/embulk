package org.embulk.spi;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import org.embulk.buffer.Buffer;

public class LineDecoder
        implements Iterable<String>
{
    private static final int INITIAL_DECODE_BUFFER_SIZE = 128;  // TODO configurable?
    private static final int MINIMUM_DECODE_BUFFER_SIZE = 128;  // TODO configurable?
    // MINIMUM_DECODE_BUFFER_SIZE must be <= INITIAL_DECODE_BUFFER_SIZE

    private static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);

    private final Iterator<Buffer> input;
    private final Newline newline;

    private int newlineSearchPosition;
    private Buffer buffer;
    private ByteBuffer byteBuffer;
    private CharsetDecoder decoder;
    private CharBuffer lineBuffer;

    public LineDecoder(Iterable<Buffer> input, LineDecoderTask task)
    {
        this.input = input.iterator();
        this.newline = task.getNewline();
        this.decoder = task.getCharset()
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)  // TODO configurable?
            .onUnmappableCharacter(CodingErrorAction.REPLACE);  // TODO configurable?
        this.lineBuffer = CharBuffer.allocate(INITIAL_DECODE_BUFFER_SIZE);
        lineBuffer.limit(0);
        this.byteBuffer = EMPTY_BYTE_BUFFER;
    }

    // TODO initialize and call LineFilterPlugin

    public String poll()
    {
        while (true) {
            if (lineBuffer.hasRemaining()) {
                int pos = searchNewline();
                if (pos >= 0) {
                    String line = lineBuffer.subSequence(0, pos - lineBuffer.position()).toString();
                    lineBuffer.position(newlineSearchPosition);
                    return line;
                }
            }

            if (!byteBuffer.hasRemaining()) {
                if (buffer != null) {
                    buffer.release();
                    buffer = null;
                }
                if (!input.hasNext()) {
                    if (byteBuffer == EMPTY_BYTE_BUFFER) {
                        // last string
                        if (lineBuffer.hasRemaining()) {
                            String line = lineBuffer.subSequence(0, lineBuffer.remaining()).toString();
                            lineBuffer.limit(lineBuffer.position());
                            return line;
                        }
                        return null;
                    }
                    byteBuffer = EMPTY_BYTE_BUFFER;
                } else {
                    buffer = input.next();
                    byteBuffer = ByteBuffer.wrap(buffer.get(), 0, buffer.limit());
                }
            }

            if (lineBuffer.capacity() - lineBuffer.limit() < MINIMUM_DECODE_BUFFER_SIZE) {
                // lineBuffer is almost full. rewind or resize buffer
                if (lineBuffer.capacity() - lineBuffer.remaining() < MINIMUM_DECODE_BUFFER_SIZE) {
                    // rewinding is insufficient to secure minium size. resizing.
                    // TODO limit maxmium possible line size to prevent OutOfMemoryError
                    CharBuffer nextLineBuffer = CharBuffer.allocate(lineBuffer.capacity() * 2);
                    newlineSearchPosition -= lineBuffer.position();
                    nextLineBuffer.put(lineBuffer);
                    nextLineBuffer.flip();
                    lineBuffer = nextLineBuffer;
                } else {
                    // rewinding
                    newlineSearchPosition -= lineBuffer.position();
                    CharBuffer slice = lineBuffer.slice();
                    lineBuffer.position(0);
                    lineBuffer.limit(slice.limit());
                    lineBuffer.put(slice);
                    lineBuffer.flip();
                }
            }

            int readingPosition = lineBuffer.position();
            lineBuffer.position(lineBuffer.limit());
            lineBuffer.limit(lineBuffer.capacity());
            CoderResult cr = decoder.decode(byteBuffer, lineBuffer, byteBuffer == EMPTY_BYTE_BUFFER);  // last byteBuffer is EMPTY_BYTE_BUFFER
            lineBuffer.limit(lineBuffer.position());
            lineBuffer.position(readingPosition);
            if (cr.isUnderflow()) {
                // ok, decode a line at the next loop
            } else if (cr.isOverflow()) {
                // rewind or resize buffer at the next loop
            } else {
                // error
                try {
                    cr.throwException();
                } catch (CharacterCodingException ex) {
                    throw new LineCharacterCodingException(ex);
                }
            }
        }
    }

    private int searchNewline()
    {
        int pos = newlineSearchPosition;
        int limit = lineBuffer.limit();
        char firstChar = newline.getFirstCharCode();
        for (; pos < limit; pos++) {
            if (lineBuffer.get(pos) == firstChar) {
                if (newline != Newline.CRLF) {
                    // LF or CR
                    newlineSearchPosition = pos + 1;
                    return pos;
                } else {
                    // CRLF
                    if (pos + 1 >= limit) {
                        // insufficient buffer
                        newlineSearchPosition = pos;
                        return -1;
                    }
                    if (lineBuffer.get(pos + 1) == newline.getSecondCharCode()) {
                        // CRLF matched
                        newlineSearchPosition = pos + 2;
                        return pos;
                    } else {
                        // CR matched but LF didn't match
                        pos++;
                    }
                }
            }
        }
        newlineSearchPosition = pos;
        return -1;
    }

    private static class Ite
            implements Iterator<String>
    {
        private final LineDecoder lineDecoder;
        private String line;

        public Ite(LineDecoder lineDecoder)
        {
            this.lineDecoder = lineDecoder;
        }

        @Override
        public boolean hasNext()
        {
            if (line != null) {
                return true;
            } else {
                line = lineDecoder.poll();
                return line != null;
            }
        }

        @Override
        public String next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String l = line;
            line = null;
            return l;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    public Iterator<String> iterator()
    {
        return new Ite(this);
    }
}

