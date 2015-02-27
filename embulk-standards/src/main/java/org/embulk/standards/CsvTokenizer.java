package org.embulk.standards;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import org.embulk.spi.util.LineDecoder;

public class CsvTokenizer
{
    static enum RecordState
    {
        NOT_END, END,
    }

    static enum ColumnState
    {
        BEGIN, VALUE, QUOTED_VALUE, AFTER_QUOTED_VALUE, FIRST_TRIM, LAST_TRIM_OR_VALUE,
    }

    private static final char END_OF_LINE = '\0';
    private static final boolean TRACE = false;

    private final char delimiter;
    private final char quote;
    private final char escape;
    private final String newline;
    private final boolean trimIfNotQuoted;
    private final long maxQuotedSizeLimit;
    private final LineDecoder input;

    private RecordState recordState = RecordState.END;  // initial state is end of a record. nextRecord() must be called first
    private long lineNumber = 0;

    private String line = null;
    private int linePos = 0;
    private boolean wasQuotedColumn = false;
    private List<String> quotedValueLines = new ArrayList<>();
    private Deque<String> unreadLines = new ArrayDeque<>();

    public CsvTokenizer(LineDecoder input, CsvParserPlugin.PluginTask task)
    {
        delimiter = task.getDelimiterChar();
        quote = task.getQuoteChar() != '\0' ? task.getQuoteChar() : '"';
        escape = task.getEscapeChar();
        newline = task.getNewline().getString();
        trimIfNotQuoted = task.getTrimIfNotQuoted();
        maxQuotedSizeLimit = task.getMaxQuotedSizeLimit();
        this.input = input;
    }

    public long getCurrentLineNumber()
    {
        return lineNumber;
    }

    // returns skipped line
    public String skipCurrentLine()
    {
        String skippedLine;
        if (quotedValueLines.isEmpty()) {
            skippedLine = line;
        } else {
            // recover lines of quoted value
            skippedLine = quotedValueLines.remove(0);  // TODO optimize performance
            unreadLines.addAll(quotedValueLines);
            unreadLines.add(line);
            lineNumber -= quotedValueLines.size();
            quotedValueLines.clear();
        }
        recordState = RecordState.END;
        return skippedLine;
    }

    public boolean nextFile()
    {
        return input.nextFile();
    }

    public boolean nextRecord()
    {
        // If at the end of record, read the next line and initialize the state
        Preconditions.checkState(recordState == RecordState.END, "too many columns");  // TODO exception class
        boolean hasNext = nextLine(true);
        if (hasNext) {
            recordState = RecordState.NOT_END;
            return true;
        } else {
            return false;
        }
    }

    private boolean nextLine(boolean ignoreEmptyLine)
    {
        while (true) {
            if (!unreadLines.isEmpty()) {
                line = unreadLines.removeFirst();
            } else {
                line = input.poll();
                if (line == null) {
                    return false;
                }
            }
            linePos = 0;
            lineNumber++;

            if (TRACE) {
                System.out.println("#MN line: " + line + " (" + lineNumber + ")");
            }

            if (!line.isEmpty() || !ignoreEmptyLine) {
                return true;
            }
        }
    }

    public String nextColumn()
    {
        Preconditions.checkState(recordState == RecordState.NOT_END, "doesn't have enough columns");  // TODO exception class

        // reset last state
        wasQuotedColumn = false;
        quotedValueLines.clear();

        // local state
        int valueStartPos = linePos;
        int valueEndPos = 0;  // initialized by VALUE state and used by LAST_TRIM_OR_VALUE and
        StringBuilder quotedValue = null;  // initial by VALUE or FIRST_TRIM state and used by QUOTED_VALUE state
        ColumnState columnState = ColumnState.BEGIN;

        while (true) {
            final char c = nextChar();
            if (TRACE) {
                System.out.println("#MN c: " + c + " (" + columnState + "," + recordState + ")");
                try { Thread.sleep(100); } catch (Exception e) {}
            }

            switch (columnState) {
                case BEGIN:
                    // TODO optimization: state is BEGIN only at the first character of a column.
                    //      this block can be out of the looop.
                    if (isDelimiter(c)) {
                        // empty value
                        return "";

                    } else if (isEndOfLine(c)) {
                        // empty value
                        recordState = RecordState.END;
                        return "";

                    } else if (isSpace(c) && trimIfNotQuoted) {
                        columnState = ColumnState.FIRST_TRIM;

                    } else if (isQuote(c)) {
                        valueStartPos = linePos;  // == 1
                        wasQuotedColumn = true;
                        quotedValue = new StringBuilder();
                        columnState = ColumnState.QUOTED_VALUE;

                    } else {
                        columnState = ColumnState.VALUE;
                    }
                    break;

                case FIRST_TRIM:
                    if (isDelimiter(c)) {
                        // empty value
                        return "";

                    } else if (isEndOfLine(c)) {
                        // empty value
                        recordState = RecordState.END;
                        return "";

                    } else if (isQuote(c)) {
                        // column has heading spaces and quoted. TODO should this be rejected?
                        valueStartPos = linePos;
                        wasQuotedColumn = true;
                        quotedValue = new StringBuilder();
                        columnState = ColumnState.QUOTED_VALUE;

                    } else if (isSpace(c)) {
                        // skip this character

                    } else {
                        valueStartPos = linePos - 1;
                        columnState = ColumnState.VALUE;
                    }
                    break;

                case VALUE:
                    if (isDelimiter(c)) {
                        return line.substring(valueStartPos, linePos - 1);

                    } else if (isEndOfLine(c)) {
                        recordState = RecordState.END;
                        return line.substring(valueStartPos, linePos);

                    } else if (isSpace(c) && trimIfNotQuoted) {
                        valueEndPos = linePos - 1;  // this is possibly end of value
                        columnState = ColumnState.LAST_TRIM_OR_VALUE;

                    // TODO not implemented yet foo""bar""baz -> [foo, bar, baz].append
                    //} else if (isQuote(c)) {
                    //    // In RFC4180, If fields are not enclosed with double quotes, then
                    //    // double quotes may not appear inside the fields. But they are often
                    //    // included in the fields. We should care about them later.

                    } else {
                        // keep VALUE state
                    }
                    break;

                case LAST_TRIM_OR_VALUE:
                    if (isDelimiter(c)) {
                        return line.substring(valueStartPos, valueEndPos);

                    } else if (isEndOfLine(c)) {
                        recordState = RecordState.END;
                        return line.substring(valueStartPos, valueEndPos);

                    } else if (isSpace(c)) {
                        // keep LAST_TRIM_OR_VALUE state

                    } else {
                        // this spaces are not trailing spaces. go back to VALUE state
                        columnState = ColumnState.BEGIN;
                    }
                    break;

                case QUOTED_VALUE:
                    if (isEndOfLine(c)) {
                        // multi-line quoted value
                        quotedValue.append(line.substring(valueStartPos, linePos));
                        quotedValue.append(newline);
                        quotedValueLines.add(line);
                        if (!nextLine(false)) {
                            throw new RuntimeException("Unexpected end of line during parsing a quoted value");  // TODO exception class
                        }
                        valueStartPos = 0;

                    } else if (isQuote(c)) {
                        char next = peekNextChar();
                        if (TRACE) {
                            System.out.println("#MN peeked c: " + next + " (" + columnState + "," + recordState + ")");
                        }
                        if (isQuote(next)) { // escaped quote
                            quotedValue.append(line.substring(valueStartPos, linePos));
                            valueStartPos = ++linePos;
                        } else {
                            quotedValue.append(line.substring(valueStartPos, linePos - 1));
                            columnState = ColumnState.AFTER_QUOTED_VALUE;
                        }

                    } else if (isEscape(c)) {  // isQuote must be checked first in case of quote == escape
                        // In RFC 4180, CSV's escape char is '\"'. But '\\' is often used.
                        char next = peekNextChar();
                        if (TRACE) {
                            System.out.println("#MN peeked c: " + next + " (" + columnState + "," + recordState + ")");
                        }
                        if (isEndOfLine(c)) {
                            // escape end of line. TODO assuming multi-line quoted value without newline?
                            quotedValue.append(line.substring(valueStartPos, linePos));
                            quotedValueLines.add(line);
                            if (!nextLine(false)) {
                                throw new RuntimeException("Unexpected end of line during parsing a quoted value");  // TODO exception class
                            }
                            valueStartPos = 0;
                        } else if (isQuote(next) || isEscape(next)) { // escaped quote
                            quotedValue.append(line.substring(valueStartPos, linePos - 1));
                            quotedValue.append(next);
                            valueStartPos = ++linePos;
                        }

                    } else {
                        if ((linePos - valueStartPos) + quotedValue.length() > maxQuotedSizeLimit) {
                            throw new QuotedSizeLimitExceededException("The size of the quoted value exceeds the limit size ("+maxQuotedSizeLimit+")");
                        }
                        // keep QUOTED_VALUE state
                    }
                    break;

                case AFTER_QUOTED_VALUE:
                    if (isDelimiter(c)) {
                        return quotedValue.toString();

                    } else if (isEndOfLine(c)) {
                        recordState = RecordState.END;
                        return quotedValue.toString();

                    } else if (isSpace(c)) {
                        // column has trailing spaces and quoted. TODO should this be rejected?

                    } else {
                        throw new RuntimeException("Unexpected extra character after quoted value");  // TODO exception class
                    }
                    break;

                default:
                    assert false;
            }
        }
    }

    public boolean wasQuotedColumn()
    {
        return wasQuotedColumn;
    }

    private char nextChar()
    {
        Preconditions.checkState(line != null, "nextColumn is called after end of file");

        if (linePos >= line.length()) {
            return END_OF_LINE;
        } else {
            return line.charAt(linePos++);
        }
    }

    private char peekNextChar()
    {
        Preconditions.checkState(line != null, "peekNextChar is called after end of file");

        if (linePos >= line.length()) {
            return END_OF_LINE;
        } else {
            return line.charAt(linePos);
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

    private boolean isEscape(char c)
    {
        return c == escape;
    }

    static class QuotedSizeLimitExceededException
            extends RuntimeException
    {
        QuotedSizeLimitExceededException(String message)
        {
            super(message);
        }
    }
}
