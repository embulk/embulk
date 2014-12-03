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

    static enum CursorMode
    {
        DEFAULT_PARSED, FIRST_TRIMMED, LAST_TRIMMED, QUOTED,
    }

    private static final char END_OF_LINE = 0;
    private static final boolean TRACE = false;

    private final char delimiter;
    private final char quote;
    private final String newline;
    private final boolean trimmedIfNotQuoted;
    private final long maxQuotedSizeLimit;

    private Iterator<String> lineDecoder;

    private ParserState parserState = ParserState.BEGIN;
    private CursorMode cursorMode = CursorMode.DEFAULT_PARSED;
    private List<String> lineBuffer = new ArrayList<>();
    private int lineBufferIndex = 0;
    private long lineNum = 0;
    private String line = null;
    private int linePos = 0, columnStartPos = 0, columnEndPos = 0;
    private boolean isQuotedColumn = false;
    private StringBuilder column = new StringBuilder();

    public CsvTokenizer(LineDecoder decoder, CsvParserTask task)
    {
        delimiter = task.getDelimiterChar();
        quote = task.getQuoteChar();
        newline = task.getNewline().getString();
        trimmedIfNotQuoted = task.getTrimmedIfNotQuoted();
        maxQuotedSizeLimit = task.getMaxQuotedSizeLimit();

        lineDecoder = decoder.iterator();
    }

    public long getCurrentLineNum()
    {
        return lineNum;
    }

    public String getCurrentUntokenizedLine() {
        StringBuilder sbuf = new StringBuilder();
        for (int i = 0; i < lineBuffer.size(); i++) {
            sbuf.append(lineBuffer.get(i));
            if (i + 1 < lineBuffer.size()) {
                sbuf.append(newline);
            }
        }
        return sbuf.toString();
    }

    public boolean hasNextRecord()
    {
        // returns true if LineDecoder has more lines.
        return !lineBuffer.isEmpty() || lineDecoder.hasNext();
    }

    public void nextRecord()
    {
        Preconditions.checkState(parserState.equals(ParserState.END), "too many columns");

        // change the parser state to 'begin'
        parserState = ParserState.BEGIN;

        // change the start/end positions to 0s
        linePos = columnStartPos = columnEndPos = 0;

        line = null;
        lineBuffer.clear();
    }

    public void skipLine()
    {
        while (!parserState.equals(ParserState.END)) {
            nextColumn();
        }
        nextRecord();
    }

    public String nextColumn()
    {
        Preconditions.checkState(!parserState.equals(ParserState.END), "doesn't have enough columns");

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
        cursorMode = CursorMode.DEFAULT_PARSED;

        boolean parseStarted = false;
        boolean loopFinished = false;
        while (!loopFinished) {
            final char c = getChar(linePos);
            if (TRACE) {
                System.out.println("#MN c: " + c + " (" + cursorMode + "," + parserState + ")");
                try { Thread.sleep(100); } catch (Exception e) {}
            }

            switch (cursorMode) {
                case FIRST_TRIMMED:
                    if (isSpace(c)) {
                        columnStartPos = columnEndPos = ++linePos;

                    } else if (isQuote(c)) {
                        throw new RuntimeException("should not rearch this control");

                    } else {
                        columnStartPos = columnEndPos = linePos;
                        linePos++;
                        cursorMode = CursorMode.DEFAULT_PARSED;

                    }
                    break;

                case DEFAULT_PARSED:
                    if (isDelimiter(c)) {
                        linePos++;
                        loopFinished = true;

                    } else if (isEndOfLine(c)) {
                        if (!parseStarted) { // BEGIN state
                            throw new RuntimeException("should not rearch this control");
                        } else {
                            parserState = ParserState.END;
                            loopFinished = true;
                        }

                    } else if (isSpace(c)) {
                        if (trimmedIfNotQuoted) {
                            if (!parseStarted) { // BEGIN state
                                columnStartPos = columnEndPos = ++linePos;
                                cursorMode = CursorMode.FIRST_TRIMMED;
                            } else {
                                linePos++;
                                cursorMode = CursorMode.LAST_TRIMMED;
                            }
                        } else {
                            columnEndPos = ++linePos;
                        }

                    } else if (isQuote(c)) {
                        if (!parseStarted) { // BEGIN state
                            isQuotedColumn = true;
                            columnStartPos = columnEndPos = ++linePos;
                            cursorMode = CursorMode.QUOTED;
                        } else {
                            throw new RuntimeException("should not rearch this control");
                            // TODO not implemented yet foo""bar""baz -> [foo, bar, baz].append
                            // In RFC4180, If fields are not enclosed with double quotes, then
                            // double quotes may not appear inside the fields. But they are often
                            // included in the fields. We should care about them later.
                        }

                    } else {
                        columnEndPos = ++linePos;

                    }
                    parseStarted = true;
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
                            System.out.println("#MN quoted c: " + next + " (" + cursorMode + "," + parserState + ")");
                        }
                        if (isQuote(next)) { // check that it's escaped quote or not.
                            column.append(line.substring(columnStartPos, columnEndPos)).append(quote);
                            columnStartPos = columnEndPos = ++linePos;
                        } else {
                            cursorMode = CursorMode.DEFAULT_PARSED; // back to 'normal' mode
                        }

                    } else {
                        columnEndPos = ++linePos;

                    }
                    break;

                case LAST_TRIMMED:
                    if (isDelimiter(c)) {
                        linePos++;
                        loopFinished = true;

                    } else if (isEndOfLine(c)) {
                        parserState = ParserState.END;
                        loopFinished = true;

                    } else if (isSpace(c)) {
                        linePos++;

                    } else if (isQuote(c)) {
                        throw new RuntimeException("should not rearch this control"); // TODO

                    } else {
                        columnEndPos = ++linePos;
                        cursorMode = CursorMode.DEFAULT_PARSED;

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
                if (l.isEmpty() && (cursorMode.equals(CursorMode.DEFAULT_PARSED))) {
                    continue;
                }

                // update the untokenized line: if current state is 'normal', the untokenized
                // line first should be cleaned up. otherwise (i mean 'quoted' mode), new line
                // is appended to current untokenized line.
                if (cursorMode.equals(CursorMode.DEFAULT_PARSED)) {
                    lineBuffer.clear();
                }
                lineBuffer.add(line);

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