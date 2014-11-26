package org.quickload.standards;

import org.quickload.spi.LineDecoder;

import java.util.Iterator;

public class CsvTokenizer
{
    static enum State
    {
        BEGIN_OF_COLUMN, FIRST_TRIM, LAST_TRIM,
        NORMAL, QUOTED, END_OF_COLUMN, END_OF_COLUMNS;
    }

    private static final char END_OF_LINE = 0;

    private final char delimiter;
    private final char quote;
    private final String newline;
    private final boolean trimIfNotQuoted;
    private final long maxQuotedColumnSize;

    private Iterator<String> lineDecoder;

    private State currentState = State.BEGIN_OF_COLUMN;
    private long currentLineNum = 0;
    private String currentLine = null;
    private int currentLinePos = 0;
    private boolean isQuotedColumn = false;
    private final StringBuilder currentColumn = new StringBuilder();
    private final StringBuilder currentUntokenizedLine = new StringBuilder();

    // @see http://tools.ietf.org/html/rfc4180
    public CsvTokenizer(LineDecoder decoder, CsvParserTask task)
    {
        delimiter = task.getDelimiterChar();
        quote = task.getQuoteChar();
        newline = task.getNewline().getString();
        trimIfNotQuoted = task.getTrimIfNotQuoted();
        maxQuotedColumnSize = task.getMaxQuotedColumnSize();

        lineDecoder = decoder.iterator();
    }

    public long getCurrentLineNum()
    {
        return currentLineNum;
    }

    public String getCurrentUntokenizedLine()
    {
        return currentUntokenizedLine.toString();
    }

    public boolean hasNextRecord()
    {
        // returns true if LineDecoder has more lines.
        return lineDecoder.hasNext();
    }

    public void nextRecord()
    {
        if (!currentState.equals(State.END_OF_COLUMNS)) {
            throw new CsvValueValidateException("too many columns");
        }

        // change the current state to 'start'
        currentState = State.BEGIN_OF_COLUMN;

        // change the current position to 0
        currentLinePos = 0;
        currentLine = null;
        currentUntokenizedLine.setLength(0);
    }

    public void skipLine()
    {
        while (!currentState.equals(State.END_OF_COLUMNS)) {
            nextColumn();
        }
        nextRecord();
    }

    public String nextColumn()
    {
        fetchNextColumn();

        if (!currentState.equals(State.END_OF_COLUMNS) &&
                !currentState.equals(State.END_OF_COLUMN)) {
            throw new CsvValueValidateException("doesn't have enough columns");
        }

        String c = currentColumn.toString();
        currentColumn.setLength(0);
        return c;
    }

    public boolean isQuotedColumn()
    {
        return isQuotedColumn;
    }

    private void fetchNextColumn()
    {
        isQuotedColumn = false;
        currentState = State.BEGIN_OF_COLUMN;

        // keep track of spaces (so leading/trailing space can be removed if required)
        int potentialSpaces = 0;

        while (true) {
            final char c = getChar();

            if (currentState.equals(State.BEGIN_OF_COLUMN)) {
                if (isDelimiter(c)) {
                    currentState = State.END_OF_COLUMN;
                    break;

                } else if (isEndOfLine(c)) {
                    currentState = State.END_OF_COLUMNS;
                    break;

                } else if (isSpace(c)) {
                    potentialSpaces += 1;
                    currentState = State.FIRST_TRIM;

                } else if (isQuote(c)) {
                    isQuotedColumn = true;
                    currentState = State.QUOTED;

                } else {
                    currentColumn.append(c);
                    currentState = State.NORMAL;

                }
            } else if (currentState.equals(State.FIRST_TRIM)) {
                if (isSpace(c)) {
                    // state is not change
                    potentialSpaces += 1;

                } else if (isQuote(c)) {
                    if (!trimIfNotQuoted) {
                        insertSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;
                    isQuotedColumn = true;
                    currentState = State.QUOTED;

                } else {
                    if (!trimIfNotQuoted) {
                        insertSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;
                    currentColumn.append(c);
                    currentState = State.NORMAL;

                }
            } else if (currentState.equals(State.NORMAL)) {
                if (isDelimiter(c)) {
                    currentState = State.END_OF_COLUMN;
                    break;

                } else if (isEndOfLine(c)) {
                    currentState = State.END_OF_COLUMNS;
                    break;

                } else if (isSpace(c)) {
                    potentialSpaces += 1;
                    currentState = State.LAST_TRIM;

                } else if (isQuote(c)) {
                    isQuotedColumn = true;
                    currentState = State.QUOTED;

                } else {
                    // state is not change
                    currentColumn.append(c);

                }
            } else if (currentState.equals(State.QUOTED)) {
                // TODO it is not rfc4180 but we should parse an escapsed quote char like \" and \\"

                if (isEndOfLine(c)) {
                    currentColumn.append(newline);
                    currentLine = null;
                    currentLinePos = 0;

                } else if (isQuote(c)) {
                    currentState = State.NORMAL; // back to 'normal' mode

                } else {
                    // state is not change
                    currentColumn.append(c);

                    if (currentColumn.length() >= maxQuotedColumnSize) {
                        throw new CsvValueValidateException("too large sized column value");
                    }

                }
            } else if (currentState.equals(State.LAST_TRIM)) {
                if (isDelimiter(c)) {
                    if (!trimIfNotQuoted) {
                        appendSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;
                    currentState = State.END_OF_COLUMN;
                    break;

                } else if (isEndOfLine(c)) {
                    if (!trimIfNotQuoted) {
                        appendSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;
                    currentState = State.END_OF_COLUMNS;
                    break;

                } else if (isSpace(c)) {
                    // state is not change
                    potentialSpaces += 1;

                } else if (isQuote(c)) {
                    potentialSpaces = 0;
                    currentState = State.QUOTED;

                } else {
                    appendSpaces(currentColumn, potentialSpaces);
                    currentColumn.append(c);
                    potentialSpaces = 0;
                    currentState = State.NORMAL;

                }
            } else {
                throw new RuntimeException("should not rearch this control");
            }
        }
    }

    private char getChar()
    {
        if (currentLine == null) {
            String line;

            while (lineDecoder.hasNext()) {
                line = lineDecoder.next();
                currentLineNum++; // increment # of line

                // if it finds empty lines with STRING currentState, they should be skipped.
                if (line.isEmpty() && (currentState.equals(State.BEGIN_OF_COLUMN))) {
                    continue;
                }

                // update the untokenized line: if current state is 'normal', the untokenized
                // line first should be cleaned up. otherwise (i mean 'quoted' mode), new line
                // is appended to current untokenized line.
                if (currentState.equals(State.BEGIN_OF_COLUMN)) {
                    currentUntokenizedLine.setLength(0);
                } else {
                    currentUntokenizedLine.append(newline); // multi lines
                }
                currentUntokenizedLine.append(line);

                if (line != null) {
                    currentLine = line;
                    break;
                }
            }
        }

        if (currentLine == null || currentLinePos >= currentLine.length()) {
            return END_OF_LINE;
        } else {
            return currentLine.charAt(currentLinePos++);
        }
    }

    private void insertSpaces(final StringBuilder sbuf, final int spaces)
    {
        for (int i = 0; i < spaces; i++) {
            sbuf.insert(0, ' ');
        }
    }

    private void appendSpaces(final StringBuilder sbuf, final int spaces)
    {
        for (int i = 0; i < spaces; i++) {
            sbuf.append(' ');
        }
    }

    private boolean isSpace(char c)
    {
        return c == ' ';
    }

    private boolean isDelimiter(char c)
    {
        return c == delimiter;
    }

    private boolean isEndOfLine(char c)
    {
        return c == END_OF_LINE;
    }

    private boolean isQuote(char c)
    {
        return c == quote;
    }

    static class CsvValueValidateException
            extends RuntimeException
    {
        CsvValueValidateException(String reason)
        {
            super(reason);
        }
    }
}