package org.embulk.spi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A {@link BufferedReader} that can specify line delimiter character from any one of CR, LF and CRLF.
 * If not specified, use original {@link BufferedReader}.
 *
 * This class is not thread-safe.
 */
class LineReader extends BufferedReader {
    private static final int UNREAD = -1;
    private final LineDelimiter lineDelimiter;
    private final char[] buffer;
    private int offset;
    private int charsRead;

    static BufferedReader of(Reader reader, LineDelimiter lineDelimiter, int bufferSize) {
        if (lineDelimiter == null) {
            return new BufferedReader(reader);
        }
        return new LineReader(reader, lineDelimiter, bufferSize);
    }

    private LineReader(Reader reader, LineDelimiter lineDelimiter, int bufferSize) {
        super(reader);
        this.lineDelimiter = lineDelimiter;
        this.buffer = new char[bufferSize];
        this.offset = UNREAD;
        this.charsRead = 0;
    }

    @Override
    public String readLine() throws IOException {
        StringBuilder line = null;
        char prevChar = Character.MIN_VALUE;
        bufferLoop:
        while (offset != UNREAD || (charsRead = this.read(buffer)) != -1) {
            if (offset == UNREAD) {
                // Initialize offset after read chars to buffer
                offset = 0;
            }
            if (line == null) {
                // Initialize line's buffer for the first loop
                line = new StringBuilder();
            }
            for (int i = offset; i < charsRead; i++) {
                char c = buffer[i];
                boolean isEol = false;
                switch (lineDelimiter) {
                    case CR:
                        if (c == '\r') {
                            Character next = readNext();
                            if (next == null || next != '\n') {
                                isEol = true;
                            }
                        }
                        break;
                    case LF:
                        if (prevChar != '\r' && c == '\n') {
                            isEol = true;
                        }
                        break;
                    case CRLF:
                        if (prevChar == '\r' && c == '\n') {
                            // Delete unnecessary CR
                            line.deleteCharAt(line.length() - 1);
                            isEol = true;
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unsupported line delimiter " + lineDelimiter);
                }
                offset++;
                if (isEol) {
                    break bufferLoop;
                }
                line.append(c);
                prevChar = c;
            }
            // Set "UNREAD" to read next chars
            offset = UNREAD;
        }

        if (line != null) {
            return line.toString();
        }
        return null;
    }

    private Character readNext() throws IOException {
        if (offset < charsRead - 1) {
            // From buffer
            return buffer[offset + 1];
        }
        // From reader
        this.mark(1);
        char[] tmp = new char[1];
        final int read = this.read(tmp);
        this.reset();
        if (read == -1) {
            return null;
        }
        return tmp[0];
    }
}
