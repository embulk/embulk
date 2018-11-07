package org.embulk.spi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A {@link BufferedReader} that can specify line delimiter character from any one of CR, LF and CRLF.
 * If not specified, follow the original {@link BufferedReader}'s behavior.
 *
 * This class is not thread-safe.
 */
class LineReader extends BufferedReader {
    private static final int UNREAD = -1;
    private final Newline lineDelimiter;   // TODO: create another enum for the option
    private final char[] buffer;
    private int offset = UNREAD;
    private int charsRead = 0;

    LineReader(Reader reader, Newline lineDelimiter, int bufferSize) {
        super(reader);
        this.lineDelimiter = lineDelimiter;
        this.buffer = new char[bufferSize];
    }

    @Override
    public String readLine() throws IOException {
        if (lineDelimiter == null) {
            return super.readLine();
        }

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
                        if (prevChar == '\r' && c != '\n') {
                            // Delete unnecessary CR and move offset back
                            line.deleteCharAt(line.length() - 1);
                            offset--;
                            isEol = true;
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
}
