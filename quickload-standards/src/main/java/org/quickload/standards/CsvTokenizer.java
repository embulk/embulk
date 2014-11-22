package org.quickload.standards;

import com.google.common.base.Objects;
import org.quickload.spi.LineDecoder;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CsvTokenizer
{
    static enum Mode
    {
        NORMAL, QUOTE, END_OF_COLUMNS_MODE;
    }

    static enum Identifier
    {
        STRING, EMPTY_STRING;
    }

    static class Token implements Cloneable
    {
        private final Identifier id;
        private final String column;
        private final boolean isQuoted;

        Token(Identifier id, String column, boolean isQuoted)
        {
            this.id = id;
            this.column = column;
            this.isQuoted = isQuoted;
        }

        public boolean isQuoted()
        {
            return isQuoted;
        }

        public boolean isEmptyColumnString()
        {
            return id.equals(Identifier.EMPTY_STRING);
        }

        public String getColumn()
        {
            return column;
        }

        @Override
        public String toString()
        {
            return Objects.toStringHelper("Token")
                    .add("id", id)
                    .add("column", column)
                    .add("isQuoted", isQuoted)
                    .toString();
        }

        @Override
        public Object clone()
        {
            return new Token(id, column, isQuoted);
        }
    }

    private static final char END_OF_LINE = 0;

    private final char delimiter;
    private final char quote;
    private final String newline;
    private final boolean surroundingSpacesNeedQuotes;

    private Iterator<String> decoder;
    private Mode mode;
    private Identifier currentId;
    private Token currentToken;
    private int currentLineNum;
    private String currentLine;
    private int currentLinePos;
    private boolean isQuotedColumn;
    private final StringBuilder currentColumn;
    private final StringBuilder currentUntokenizedLine;

    // @see http://tools.ietf.org/html/rfc4180
    public CsvTokenizer(LineDecoder decoder, CsvParserTask task)
    {
        delimiter = task.getDelimiterChar();
        quote = task.getQuoteChar();
        newline = task.getNewline().getString();
        surroundingSpacesNeedQuotes = false; // TODO

        this.decoder = decoder.iterator();
        mode = Mode.NORMAL;
        currentToken = null;
        currentId = Identifier.STRING;
        isQuotedColumn = false;
        currentColumn = new StringBuilder();
        currentLineNum = 0;
        currentLinePos = 0;
        currentUntokenizedLine = new StringBuilder();
    }

    public int getCurrentLineNum()
    {
        return currentLineNum;
    }

    public String getCurrentUntokenizedLine()
    {
        return currentUntokenizedLine.toString();
    }

    public boolean hasNextRecord()
    {
        return decoder.hasNext();
    }

    public boolean hasNextToken()
    {
        if (currentToken != null) {
            return true;
        } else {
            poll();
            return currentToken != null;
        }
    }

    public Token nextToken()
    {
        if (!hasNextToken()) {
            throw new NoSuchElementException();
        }

        Token token = (Token)currentToken.clone(); // deep copy
        currentToken = null;
        return token;
    }

    private void poll()
    {
        if (mode.equals(Mode.END_OF_COLUMNS_MODE)) {
            mode = Mode.NORMAL;
            return;
        }

        // TODO check QUOTE_MODE

        // keep track of spaces (so leading/trailing space can be removed if required)
        int potentialSpaces = 0;

        while (true) {
            final char c = getChar();

            if (mode.equals(Mode.NORMAL)) {
                if (isDelimiter(c)) {
                    // Save the column (trim trailing space if required) then
                    // continue to next character.
                    if (!surroundingSpacesNeedQuotes) {
                        appendSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;
                    if (currentColumn.length() == 0) {
                        currentId = Identifier.EMPTY_STRING;
                    }
                    break;

                } else if (isEndOfLine(c)) {
                    // Save the column (trim trailing space if required) then
                    // continue to next character.
                    if (!surroundingSpacesNeedQuotes) {
                        appendSpaces(currentColumn, potentialSpaces);
                    }
                    potentialSpaces = 0;
                    mode = Mode.END_OF_COLUMNS_MODE;
                    break;

                } else if (isSpace(c)) {
                    potentialSpaces += 1;

                } else if (isQuote(c)) {
                    mode = Mode.QUOTE;
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
                    mode = Mode.NORMAL;
                    // reset ready for next multi-line cell

                } else if (isEndOfLine(c)) {
                    currentColumn.append(newline);

                } else {
                    currentColumn.append(c);
                }
            }
        }

        currentToken = new Token(currentId, currentColumn.toString(), isQuotedColumn);
        currentId = Identifier.STRING;
        currentColumn.setLength(0);
        isQuotedColumn = false;
    }

    private char getChar()
    {
        if (currentLine == null) {
            String line;

            while (decoder.hasNext()) {
                line = decoder.next();
                //System.out.println("line: "+line);
                currentLineNum++; // increment # of line

                // if it finds empty lines with STRING mode, they should be skipped.
                if (line.isEmpty() && mode.equals(Mode.NORMAL)) {
                    continue;
                }

                // update the untokenized line:
                // if STRING mode, untokenized line first should be cleaned up. otherwise,
                // (it means QUOTE mode), new line is appended to current untokenized line.
                if (mode.equals(Mode.NORMAL)) {
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