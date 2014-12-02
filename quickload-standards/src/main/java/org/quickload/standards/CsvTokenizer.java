package org.quickload.standards;

import com.google.common.base.Preconditions;
import org.quickload.spi.LineDecoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CsvTokenizer
{
    static enum ParserState
    {
        BEGIN, END,
    }

    static enum CursorState
    {
        BEGIN, PARSED, FIRST_TRIM, LAST_TRIM, QUOTED,
    }

    private static final char END_OF_LINE = 0;
    private static final boolean TRACE = false;

    private final char delimiter;
    private final char quote;
    private final String newline;
    private final boolean trimIfNotQuoted;
    private final long maxQuotedColumnSize;

    private Iterator<String> lineDecoder;

    private ParserState pState = ParserState.BEGIN;
    private CursorState cState = CursorState.BEGIN;
    private long lineNum = 0;
    private String line = null;
    private int linePos = 0, columnStartPos = 0, columnEndPos = 0;
    private boolean isQuotedColumn = false;
    private StringBuilder column = new StringBuilder();
    private List<String> untokenizedLines = new ArrayList<>();

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
        return lineNum;
    }

    public String getCurrentUntokenizedLine() {
        StringBuilder sbuf = new StringBuilder();
        for (int i = 0; i < untokenizedLines.size(); i++) {
            sbuf.append(untokenizedLines.get(i));
            if (i + 1 < untokenizedLines.size()) {
                sbuf.append(newline);
            }
        }
        return sbuf.toString();
    }

    public boolean hasNextRecord()
    {
        // returns true if LineDecoder has more lines.
        return !untokenizedLines.isEmpty() || lineDecoder.hasNext();
    }

    public void nextRecord()
    {
        Preconditions.checkState(pState.equals(ParserState.END), "too many columns");

        // change the parser state to 'begin'
        pState = ParserState.BEGIN;

        // change the start/end positions to 0s
        linePos = columnStartPos = columnEndPos = 0;

        line = null;
        untokenizedLines.clear();
    }

    public void skipLine()
    {
        while (!pState.equals(ParserState.END)) {
            nextColumn();
        }
        nextRecord();
    }

    public String nextColumn()
    {
        Preconditions.checkState(!pState.equals(ParserState.END), "doesn't have enough columns");

        // fetch and parse next column
        fetchNextColumn();

        column.append(line.substring(columnStartPos, columnEndPos));
        String c = column.toString();
        column.setLength(0);
        if (TRACE) {
            System.out.println("#MN column: " + c);
        }
        return c;
    }

    public boolean isQuotedColumn()
    {
        return isQuotedColumn;
    }

    // @see http://tools.ietf.org/html/rfc4180
    private void fetchNextColumn()
    {
        isQuotedColumn = false;
        columnStartPos = columnEndPos = linePos;
        cState = CursorState.BEGIN;

        boolean loopFinished = false;
        while (!loopFinished) {
            final char c = getChar(linePos);
            if (TRACE) {
                System.out.println("#MN c: " + c + " (" + cState + "," + pState + ")");
                try { Thread.sleep(100); } catch (Exception e) {}
            }

            switch (cState) {
                case BEGIN:
                    if (isDelimiter(c)) {
                        linePos++;
                        loopFinished = true;

                    } else if (isEndOfLine(c)) {
                        throw new RuntimeException(); // TODO not come here

                    } else if (isSpace(c)) {
                        if (trimIfNotQuoted) {
                            columnStartPos = columnEndPos = ++linePos;
                            cState = CursorState.FIRST_TRIM;
                        } else {
                            throw new RuntimeException(); // TODO not come here
                        }

                    } else if (isQuote(c)) {
                        isQuotedColumn = true;
                        columnStartPos = columnEndPos = ++linePos;
                        cState = CursorState.QUOTED;

                    } else {
                        columnEndPos = ++linePos;
                        cState = CursorState.PARSED;

                    }
                    break;

                case FIRST_TRIM:
                    if (isSpace(c)) {
                        columnStartPos = columnEndPos = ++linePos;

                    } else if (isQuote(c)) {
                        throw new RuntimeException(); // TODO not come here

                    } else {
                        columnStartPos = columnEndPos = linePos;
                        linePos++;
                        cState = CursorState.PARSED;

                    }
                    break;

                case PARSED:
                    if (isDelimiter(c)) {
                        linePos++;
                        loopFinished = true;

                    } else if (isEndOfLine(c)) {
                        pState = ParserState.END;
                        loopFinished = true;

                    } else if (isSpace(c)) {
                        if (trimIfNotQuoted) {
                            cState = CursorState.LAST_TRIM;
                            linePos++;
                        } else {
                            columnEndPos = ++linePos;
                        }

                    } else if (isQuote(c)) {
                        // TODO not implemented yet foo""bar""baz -> [foo, bar, baz].append
                        throw new RuntimeException(); // TODO not come here

                    } else {
                        columnEndPos = ++linePos;

                    }
                    break;

                case QUOTED:
                    // TODO it is not rfc4180 but we should parse an escapsed quote char like \" and \\"

                    if (isEndOfLine(c)) {
                        column.append(line.substring(columnStartPos, columnEndPos)).append(newline);
                        line = null;
                        columnStartPos = columnEndPos = linePos = 0;

                    } else if (isQuote(c)) {
                        linePos++;
                        char next = getChar(linePos);
                        if (TRACE) {
                            System.out.println("#MN quoted c: " + next + " (" + cState + "," + pState + ")");
                        }
                        if (isQuote(next)) { // escaped quote
                            column.append(line.substring(columnStartPos, columnEndPos)).append(quote);
                            columnStartPos = columnEndPos = ++linePos;
                        } else {
                            cState = CursorState.PARSED; // back to 'normal' mode
                        }

                    } else {
                        columnEndPos = ++linePos;

                    }
                    break;

                case LAST_TRIM:
                    if (isDelimiter(c)) {
                        linePos++;
                        loopFinished = true;

                    } else if (isEndOfLine(c)) {
                        pState = ParserState.END;
                        loopFinished = true;

                    } else if (isSpace(c)) {
                        linePos++;

                    } else if (isQuote(c)) {
                        throw new RuntimeException(); // TODO not come here

                    } else {
                        columnEndPos = ++linePos;
                        cState = CursorState.PARSED;

                    }
                    break;

                default:
                    throw new RuntimeException("should not rearch this control");
            }
        }
    }

    private char getChar(final int pos)
    {
        if (line == null) {
            String l;

            while (lineDecoder.hasNext()) {
                l = lineDecoder.next();
                lineNum++;
                if (TRACE) {
                    System.out.println("#MN line: " + l + " (" + lineNum + ")");
                }

                // if it finds empty lines with BEGIN state, they should be skipped.
                if (l.isEmpty() && (cState.equals(CursorState.BEGIN))) {
                    continue;
                }

                // update the untokenized line: if current state is 'normal', the untokenized
                // line first should be cleaned up. otherwise (i mean 'quoted' mode), new line
                // is appended to current untokenized line.
                if (cState.equals(CursorState.BEGIN)) {
                    untokenizedLines.clear();
                }
                untokenizedLines.add(line);

                if (l != null) {
                    line = l;
                    break;
                }
            }
        }

        if (line == null || pos >= line.length()) {
            return END_OF_LINE;
        } else {
            return line.charAt(pos);
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