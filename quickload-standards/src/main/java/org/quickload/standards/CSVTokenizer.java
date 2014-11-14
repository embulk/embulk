package org.quickload.standards;

import org.quickload.spi.LineDecoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CsvTokenizer implements Iterable<List<String>> {
    static enum State
    {
        NORMAL_MODE, QUOTE_MODE;
    }

    private final char delimiter;
    private final char quote;
    private final String newline;
    private final boolean surroundingSpacesNeedQuotes;

    private LineDecoder decoder;
    private State state;
    private int currentLineNum;
    private final StringBuilder currentValue;
    private final StringBuilder currentLine;
    private final List<String> currentRecord;

    public CsvTokenizer(LineDecoder decoder, CsvParserTask task)
    {
        delimiter = task.getDelimiterChar();
        quote = task.getQuoteChar();
        switch (task.getNewline()) { // TODO reuse setting of newline
            case "CRLF":
                newline = "\r\n";
                break;
            case "LF":
                newline = "\n";
                break;
            case "CR":
                newline = "\r";
                break;
            default:
                throw new IllegalArgumentException("in.parser.newline must be either of CRLF, LF, or CR");
        }
        surroundingSpacesNeedQuotes = false; // TODO

        this.decoder = decoder;
        state = State.NORMAL_MODE;
        currentLineNum = 0;
        currentValue = new StringBuilder();
        currentLine = new StringBuilder();
        currentRecord = new ArrayList<String>();
    }

    public int getCurrentLineNum()
    {
        return currentLineNum;
    }

    public String getCurrentLine()
    {
        return currentLine.toString();
    }

    // see http://tools.ietf.org/html/rfc4180
    private List<String> poll()
    {
        currentRecord.clear();
        currentValue.setLength(0);
        currentLine.setLength(0);

        // process each character in the line, catering for surrounding quotes (QUOTE_MODE)
        state = State.NORMAL_MODE;
        // the line number where a potential multi-line cell starts
        int quoteScopeStartingLine = -1;
        // keep track of spaces (so leading/trailing space can be removed if required)
        int potentialSpaces = 0;

        for (String line : decoder) {
            currentLineNum++; // increment # of line

            // if it finds empty lines with NORMAL_MODE, they should be skipped.
            if (line.isEmpty() && state.equals(State.NORMAL_MODE)) {
                continue;
            }

            // update the untokenized line
            currentLine.append(line);
            if (quoteScopeStartingLine > 0) {
                currentLine.append(newline); // multi lines
            }

            for (int i = 0; i < line.length(); i++) {
                final char c = line.charAt(i);

                if (state.equals(State.NORMAL_MODE)) {
                    if (c == delimiter) {
                        // Save the column (trim trailing space if required) then
                        // continue to next character.
                        if (!surroundingSpacesNeedQuotes) {
                            appendSpaces(currentValue, potentialSpaces);
                        }
                        potentialSpaces = 0;

                        currentRecord.add(currentValue.length() > 0 ?
                                currentValue.toString() : null); // empty cell "" is converted to null.
                        currentValue.setLength(0);
                    } else if (c == ' ') {
                        potentialSpaces += 1;

                    } else if (c == quote) {
                        state = State.QUOTE_MODE;
                        quoteScopeStartingLine = getCurrentLineNum();

                        // cater for spaces before a quoted section
                        if(!surroundingSpacesNeedQuotes || currentValue.length() > 0 ) {
                            appendSpaces(currentValue, potentialSpaces);
                        }
                        potentialSpaces = 0;
                    } else {
                        // add any required spaces (but trim any leading spaces if
                        // surrounding spaces need quotes), add the character, then
                        // continue to next character.
                        if(!surroundingSpacesNeedQuotes || currentValue.length() > 0 ) {
                            appendSpaces(currentValue, potentialSpaces);
                        }
                        potentialSpaces = 0;
                        currentValue.append(c);
                    }
                } else { // QUOTE_MODE
                    // TODO it is not rfc4180 but we should parse an escapsed quote char like \" and \\"

                    if (c == quote) {
                        state = State.NORMAL_MODE;
                        // reset ready for next multi-line cell
                        quoteScopeStartingLine = -1;
                    } else {
                        currentValue.append(c);
                    }
                }
            }

            if (state.equals(State.NORMAL_MODE)) {
                if (!surroundingSpacesNeedQuotes) {
                    appendSpaces(currentValue, potentialSpaces);
                }
                potentialSpaces = 0;

                currentRecord.add(currentValue.length() > 0 ?
                        currentValue.toString() : null); // empty cell "" is converted to null.
                currentValue.setLength(0);

                return currentRecord;
            } else { // QUOTE_MODE
                currentValue.append(newline);
            }
        }

        // TODO some assertion is needed like state assertion

        if (!currentRecord.isEmpty()) {
            return currentRecord;
        }

        return null;
    }

    private static void appendSpaces(final StringBuilder sbuf, final int spaces)
    {
        for (int i = 0; i < spaces; i++) {
            sbuf.append(' ');
        }
    }

    @Override
    public Iterator<List<String>> iterator()
    {
        return new Ite(this);
    }

    private static class Ite implements Iterator<List<String>>
    {
        private final CsvTokenizer tokenizer;
        private List<String> record;

        Ite(CsvTokenizer tokenizer)
        {
            this.tokenizer = tokenizer;
        }

        @Override
        public boolean hasNext()
        {
            if (record != null) {
                return true;
            } else {
                record = tokenizer.poll();
                return record != null;
            }
        }

        @Override
        public List<String> next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            List<String> r = record;
            record = null; // TODO
            return r;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}

