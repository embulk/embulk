package org.quickload.standards;

import org.quickload.spi.LineDecoder;

import java.util.Iterator;

public class CsvTokenizer implements Iterator<String>
{
    static enum State
    {
        NORMAL, QUOTE, END_OF_COLUMNS;
    }

    private static final char END_OF_LINE = 0;

    private final char delimiter;
    private final char quote;
    private final String newline;
    private final boolean surroundingSpacesNeedQuotes;
    private final long maxColumnSize;

    private Iterator<String> lineDecoder;

    private State currentState;
    private long currentLineNum;
    private String currentLine;
    private int currentLinePos;
    private boolean isQuotedColumn;
    private final StringBuilder currentColumn;
    private final StringBuilder currentUntokenizedLine;

    // @see http://tools.ietf.org/html/rfc4180
    public CsvTokenizer(LineDecoder lineDecoder, CsvParserTask task)
    {
        delimiter = task.getDelimiterChar();
        quote = task.getQuoteChar();
        newline = task.getNewline().getString();
        surroundingSpacesNeedQuotes = false; // TODO
        maxColumnSize = 128*1024*1024; // 128MB TODO

        this.lineDecoder = lineDecoder.iterator();
        currentState = State.NORMAL;
        isQuotedColumn = false;
        currentColumn = new StringBuilder();
        currentLineNum = 0;
        currentLinePos = 0;
        currentUntokenizedLine = new StringBuilder();
    }

    public long getCurrentLineNum()
    {
        return currentLineNum;
    }

    public String getCurrentUntokenizedLine()
    {
        return currentUntokenizedLine.toString();
    }

    @Override
    public boolean hasNext()
    {
        // returns true if LineDecoder has more lines.
        boolean flag = lineDecoder.hasNext();
        return flag;
        //return lineDecoder.hasNext();
    }

    @Override
    public String next()
    {
        // the method name is ambiguous. users might be confusing that
        // it is for lines or columns.
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public void skipLine()
    {
        while (hasNextColumn()) { // ad-hoc implementation
            nextColumn();
        }
    }

    public boolean hasNextColumn()
    {
        if (currentState.equals(State.END_OF_COLUMNS)) {
            currentState = State.NORMAL;
            return false;
        }

        poll();
        return true;
    }

    public boolean isQuotedColumn()
    {
        return isQuotedColumn;
    }

    public String nextColumn()
    {
        String c = currentColumn.toString();
        currentColumn.setLength(0);
        return c;
    }

    private void poll()
    {
        // TODO check QUOTE_MODE

        isQuotedColumn = false;

        // keep track of spaces (so leading/trailing space can be removed if required)
        int potentialSpaces = 0;

        while (true) {
            final char c = getChar();

            if (currentState.equals(State.NORMAL)) {
                if (isDelimiter(c)) {
                    // Save the column (trim trailing space if required) then
                    // continue to next character.
                    if (!surroundingSpacesNeedQuotes) {
                        appendSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;
                    break;

                } else if (isEndOfLine(c)) {
                    // Save the column (trim trailing space if required) then
                    // continue to next character.
                    if (!surroundingSpacesNeedQuotes) {
                        appendSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;
                    currentState = State.END_OF_COLUMNS;
                    break;

                } else if (isSpace(c)) {
                    potentialSpaces += 1;

                } else if (isQuote(c)) {
                    currentState = State.QUOTE;
                    isQuotedColumn = true;

                    // cater for spaces before a quoted section
                    if(!surroundingSpacesNeedQuotes || currentColumn.length() > 0 ) {
                        appendSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;

                } else {
                    // add any required spaces (but trim any leading spaces if
                    // surrounding spaces need quotes), add the character, then
                    // continue to next character.
                    if(!surroundingSpacesNeedQuotes || currentColumn.length() > 0 ) {
                        appendSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;
                    currentColumn.append(c);

                }
            } else { // QUOTE_MODE
                // TODO it is not rfc4180 but we should parse an escapsed quote char like \" and \\"

                if (isQuote(c)) {
                    currentState = State.NORMAL;
                    // reset ready for next multi-line cell

                } else if (isEndOfLine(c)) {
                    currentColumn.append(newline);

                } else {
                    currentColumn.append(c);
                }
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
                if (line.isEmpty() && currentState.equals(State.NORMAL)) {
                    continue;
                }

                // update the untokenized line:
                // if STRING currentState, untokenized line first should be cleaned up. otherwise,
                // (it means QUOTE currentState), new line is appended to current untokenized line.
                if (currentState.equals(State.NORMAL)) {
                    currentUntokenizedLine.setLength(0);
                } else {
                    currentUntokenizedLine.append(newline); // multi lines
                }
                currentUntokenizedLine.append(line);

                if (line != null) {
                    currentLine = line;
                    currentLinePos = 0;
                    break;
                }
            }
        }

        if (currentLine == null || currentLinePos >= currentLine.length()) {
            currentLine = null;
            return END_OF_LINE;
        } else {
            return currentLine.charAt(currentLinePos++);
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
}