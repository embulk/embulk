package org.embulk.standards;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Column;
import org.embulk.spi.Schema;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Exec;
import org.embulk.spi.FileOutput;
import org.embulk.spi.util.LineEncoder;

import org.embulk.spi.util.Newline;
import java.util.Map;

public class CsvFormatterPlugin
        implements FormatterPlugin
{
    public enum QuotePolicy
    {
        ALL("ALL"),
        MINIMAL("MINIMAL"),
        NONE("NONE");

        private final String string;

        private QuotePolicy(String string)
        {
            this.string = string;
        }

        public String getString()
        {
            return string;
        }
    }

    public interface PluginTask
            extends LineEncoder.EncoderTask, TimestampFormatter.FormatterTask
    {
        @Config("header_line")
        @ConfigDefault("true")
        public boolean getHeaderLine();

        @Config("delimiter")
        @ConfigDefault("\",\"")
        public char getDelimiterChar();

        @Config("quote")
        @ConfigDefault("\"\\\"\"")
        public char getQuoteChar();

        @Config("quote_policy")
        @ConfigDefault("\"MINIMAL\"")
        public QuotePolicy getQuotePolicy();

        @Config("escape")
        @ConfigDefault("\"\\\\\"")
        public char getEscapeChar();

        @Config("newline_in_field")
        @ConfigDefault("\"LF\"")
        public Newline getNewlineInField();
    }

    @Override
    public void transaction(ConfigSource config, Schema schema,
            FormatterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
    }

    private static Map<Integer, TimestampFormatter> newTimestampFormatters(
            TimestampFormatter.FormatterTask task, Schema schema)
    {
        ImmutableMap.Builder<Integer, TimestampFormatter> builder = new ImmutableBiMap.Builder<>();
        for (Column column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                TimestampType tt = (TimestampType) column.getType();
                builder.put(column.getIndex(), new TimestampFormatter(tt.getFormat(), task));
            }
        }
        return builder.build();
    }

    @Override
    public PageOutput open(TaskSource taskSource, final Schema schema,
            FileOutput output)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        final LineEncoder encoder = new LineEncoder(output, task);
        final char delimiter = task.getDelimiterChar();
        final QuotePolicy quotePolicy = task.getQuotePolicy();
        final char quote = task.getQuoteChar() != '\0' ? task.getQuoteChar() : '"';
        final char escape = task.getEscapeChar();
        final String newlineInField = task.getNewlineInField().getString();

        // create a file
        encoder.nextFile();

        // write header
        if (task.getHeaderLine()) {
            writeHeader(schema, encoder, delimiter, quotePolicy, quote, escape, newlineInField);
        }

        final PageReader pageReader = new PageReader(schema);

        final ColumnVisitor rowWriter;
        switch (quotePolicy) {
        case NONE:
            rowWriter = new QuoteNoneRowWriter(pageReader, task, encoder, schema);
            break;
        case ALL:
            rowWriter = new QuoteAllRowWriter(pageReader, task, encoder, schema);
            break;
        case MINIMAL:
            rowWriter = new QuoteMinimalRowWriter(pageReader, task, encoder, schema);
            break;
        default:
            throw new AssertionError();
        }

        return new PageOutput() {
            public void add(Page page)
            {
                pageReader.setPage(page);
                while (pageReader.nextRecord()) {
                    schema.visitColumns(rowWriter);
                    encoder.addNewLine();
                }
            }

            public void finish()
            {
                encoder.finish();
            }

            public void close()
            {
                encoder.close();
            }
        };
    }

    private void writeHeader(Schema schema, LineEncoder encoder, char delimiter, QuotePolicy policy, char quote, char escape, String newline)
    {
        String delimiterString = String.valueOf(delimiter);
        for (Column column : schema.getColumns()) {
            if (column.getIndex() != 0) {
                encoder.addText(delimiterString);
            }
            encoder.addText(setEscapeAndQuoteValue(column.getName(), delimiter, policy, quote, escape, newline));
        }
        encoder.addNewLine();
    }

    private String setEscapeAndQuoteValue(String v, char delimiter, QuotePolicy policy, char quote, char escape, String newline)
    {
        StringBuilder escapedValue = new StringBuilder();
        char previousChar = ' ';

        boolean isRequireQuote = policy == QuotePolicy.ALL ? true : false;

        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);

            if (c == quote) {
                escapedValue.append(escape);
                escapedValue.append(c);
                isRequireQuote = true;
            } else if (c == '\r') {
                if (policy == QuotePolicy.NONE) {
                    escapedValue.append(escape);
                }
                escapedValue.append(newline);
                isRequireQuote = true;
            } else if (c == '\n') {
                if (previousChar != '\r') {
                    if (policy == QuotePolicy.NONE) {
                        escapedValue.append(escape);
                    }
                    escapedValue.append(newline);
                    isRequireQuote = true;
                }
            } else if (c == delimiter) {
                if (policy == QuotePolicy.NONE) {
                    escapedValue.append(escape);
                }
                escapedValue.append(c);
                isRequireQuote = true;
            } else {
                escapedValue.append(c);
            }
            previousChar = c;
        }

        if (policy != QuotePolicy.NONE && isRequireQuote) {
            StringBuilder sb = new StringBuilder();
            sb.append(quote);
            sb.append(escapedValue);
            sb.append(quote);
            return sb.toString();
        } else {
            return escapedValue.toString();
        }
    }

    private static abstract class AbstractRowWriter
            implements ColumnVisitor
    {
        protected final PageReader pageReader;
        protected final LineEncoder encoder;
        protected final Map<Integer, TimestampFormatter> timestampFormatters;
        protected final String delimiterString;
        protected final char delimiter;
        protected final char quote;
        protected final char escape;
        protected final String newlineInField;

        public AbstractRowWriter(PageReader pageReader, PluginTask task, LineEncoder encoder, Schema schema)
        {
            this.pageReader = pageReader;
            this.encoder = encoder;
            this.timestampFormatters = newTimestampFormatters(task, schema);
            this.delimiter = task.getDelimiterChar();
            this.quote = task.getQuoteChar() != '\0' ? task.getQuoteChar() : '"';
            this.escape = task.getEscapeChar();
            this.newlineInField = task.getNewlineInField().getString();
            this.delimiterString = String.valueOf(delimiter);
        }

        protected void addDelimiter(Column column)
        {
            if (column.getIndex() != 0) {
                encoder.addText(delimiterString);
            }
        }

        protected void addValue(String v)
        {
            encoder.addText(v);
        }

        public abstract void booleanColumn(Column column);
        public abstract void longColumn(Column column);
        public abstract void doubleColumn(Column column);
        public abstract void stringColumn(Column column);
        public abstract void timestampColumn(Column column);
    }

    private static class QuoteNoneRowWriter
            extends AbstractRowWriter
    {
        public QuoteNoneRowWriter(PageReader pageReader, PluginTask task, LineEncoder encoder, Schema schema)
        {
            super(pageReader, task, encoder, schema);
        }

        public void booleanColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addValue(Boolean.toString(pageReader.getBoolean(column)));
            } else {
                addEmptyValue();
            }
        }

        public void longColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addValue(Long.toString(pageReader.getLong(column)));
            } else {
                addEmptyValue();
            }
        }

        public void doubleColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addValue(Double.toString(pageReader.getDouble(column)));
            } else {
                addEmptyValue();
            }
        }

        public void stringColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addEscapedValue(pageReader.getString(column));
            } else {
                addEmptyValue();
            }
        }

        public void timestampColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                Timestamp value = pageReader.getTimestamp(column);
                addEscapedValue(timestampFormatters.get(column.getIndex()).format(value));
            } else {
                addEmptyValue();
            }
        }

        private void addEmptyValue()
        { }

        private void addEscapedValue(String v)
        {
            StringBuilder escapedValue = new StringBuilder();
            char previousChar = ' ';

            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);

                if (c == quote) {
                    escapedValue.append(escape);
                    escapedValue.append(c);
                } else if (c == '\r') {
                    escapedValue.append(escape);
                    escapedValue.append(newlineInField);
                } else if (c == '\n') {
                    if (previousChar != '\r') {
                        escapedValue.append(escape);
                        escapedValue.append(newlineInField);
                    }
                } else if (c == delimiter) {
                    escapedValue.append(escape);
                    escapedValue.append(c);
                } else {
                    escapedValue.append(c);
                }
                previousChar = c;
            }

            encoder.addText(escapedValue.toString());
        }
    }

    private static class QuoteAllRowWriter
            extends AbstractRowWriter
    {
        private final String quoteString;

        public QuoteAllRowWriter(PageReader pageReader, PluginTask task, LineEncoder encoder, Schema schema)
        {
            super(pageReader, task, encoder, schema);
            this.quoteString = new String(new char[] { quote });
        }

        public void booleanColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addQuotedValue(Boolean.toString(pageReader.getBoolean(column)));
            } else {
                addEmptyValue();
            }
        }

        public void longColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addQuotedValue(Long.toString(pageReader.getLong(column)));
            } else {
                addEmptyValue();
            }
        }

        public void doubleColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addQuotedValue(Double.toString(pageReader.getDouble(column)));
            } else {
                addEmptyValue();
            }
        }

        public void stringColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addQuotedAndEscapedValue(pageReader.getString(column));
            } else {
                addEmptyValue();
            }
        }

        public void timestampColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                Timestamp value = pageReader.getTimestamp(column);
                addQuotedAndEscapedValue(timestampFormatters.get(column.getIndex()).format(value));
            } else {
                addEmptyValue();
            }
        }

        private void addEmptyValue()
        { }

        private void addQuotedValue(String v)
        {
            encoder.addText(quoteString);
            encoder.addText(v);
            encoder.addText(quoteString);
        }

        private void addQuotedAndEscapedValue(String v)
        {
            StringBuilder escapedValue = new StringBuilder();
            escapedValue.append(quote);

            char previousChar = ' ';

            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);

                if (c == quote) {
                    escapedValue.append(escape);
                    escapedValue.append(c);
                } else if (c == '\r') {
                    escapedValue.append(newlineInField);
                } else if (c == '\n') {
                    if (previousChar != '\r') {
                        escapedValue.append(newlineInField);
                    }
                } else if (c == delimiter) {
                    escapedValue.append(c);
                } else {
                    escapedValue.append(c);
                }
                previousChar = c;
            }

            escapedValue.append(quote);
            encoder.addText(escapedValue.toString());
        }
    }

    private static class QuoteMinimalRowWriter
            extends AbstractRowWriter
    {
        private final String quoteString;

        public QuoteMinimalRowWriter(PageReader pageReader, PluginTask task, LineEncoder encoder, Schema schema)
        {
            super(pageReader, task, encoder, schema);
            this.quoteString = new String(new char[] { quote });
        }

        public void booleanColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addValue(Boolean.toString(pageReader.getBoolean(column)));
            } else {
                addEmptyValue();
            }
        }

        public void longColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addValue(Long.toString(pageReader.getLong(column)));
            } else {
                addEmptyValue();
            }
        }

        public void doubleColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addValue(Double.toString(pageReader.getDouble(column)));
            } else {
                addEmptyValue();
            }
        }

        public void stringColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                addEscapeAndQuoteValue(pageReader.getString(column));
            } else {
                addEmptyValue();
            }
        }

        public void timestampColumn(Column column)
        {
            addDelimiter(column);
            if (!pageReader.isNull(column)) {
                Timestamp value = pageReader.getTimestamp(column);
                addEscapeAndQuoteValue(timestampFormatters.get(column.getIndex()).format(value));
            } else {
                addEmptyValue();
            }
        }

        private void addEmptyValue()
        { }

        private void addEscapeAndQuoteValue(String v)
        {
            StringBuilder escapedValue = new StringBuilder();
            char previousChar = ' ';

            boolean isRequireQuote = false;

            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);

                if (c == quote) {
                    escapedValue.append(escape);
                    escapedValue.append(c);
                    isRequireQuote = true;
                } else if (c == '\r') {
                    escapedValue.append(newlineInField);
                    isRequireQuote = true;
                } else if (c == '\n') {
                    if (previousChar != '\r') {
                        escapedValue.append(newlineInField);
                        isRequireQuote = true;
                    }
                } else if (c == delimiter) {
                    escapedValue.append(c);
                    isRequireQuote = true;
                } else {
                    escapedValue.append(c);
                }
                previousChar = c;
            }

            if (isRequireQuote) {
                encoder.addText(quoteString);
                encoder.addText(escapedValue.toString());
                encoder.addText(quoteString);
            } else {
                encoder.addText(escapedValue.toString());
            }
        }
    }
}
