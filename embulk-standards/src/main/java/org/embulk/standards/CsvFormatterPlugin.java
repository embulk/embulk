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

import java.util.Map;

public class CsvFormatterPlugin
        implements FormatterPlugin
{
    public interface PluginTask
            extends LineEncoder.EncoderTask, TimestampFormatter.FormatterTask
    {
        @Config("header_line")
        @ConfigDefault("true")
        public boolean getHeaderLine();
    }

    @Override
    public void transaction(ConfigSource config, Schema schema,
            FormatterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
    }

    private Map<Integer, TimestampFormatter> newTimestampFormatters(
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
        final PluginTask task = taskSource.loadTask(PluginTask.class);
        final LineEncoder encoder = new LineEncoder(output, task);
        final Map<Integer, TimestampFormatter> timestampFormatters =
                newTimestampFormatters(task, schema);

        // create a file
        encoder.nextFile();

        // write header
        if (task.getHeaderLine()) {
            writeHeader(schema, encoder);
        }

        return new PageOutput() {
            private final PageReader pageReader = new PageReader(schema, false);

            public void add(Page page)
            {
                pageReader.setPage(page);
                while (pageReader.nextRecord()) {
                    schema.visitColumns(new ColumnVisitor() {
                        public void booleanColumn(Column column)
                        {
                            addDelimiter(column);
                            if (!pageReader.isNull(column)) {
                                encoder.addText(Boolean.toString(pageReader.getBoolean(column)));
                            }
                        }

                        public void longColumn(Column column)
                        {
                            addDelimiter(column);
                            if (!pageReader.isNull(column)) {
                                encoder.addText(Long.toString(pageReader.getLong(column)));
                            }
                        }

                        public void doubleColumn(Column column)
                        {
                            addDelimiter(column);
                            if (!pageReader.isNull(column)) {
                                encoder.addText(Double.toString(pageReader.getDouble(column)));
                            }
                        }

                        public void stringColumn(Column column)
                        {
                            addDelimiter(column);
                            if (!pageReader.isNull(column)) {
                                // TODO escape and quoting
                                encoder.addText(pageReader.getString(column));
                            }
                        }

                        public void timestampColumn(Column column)
                        {
                            addDelimiter(column);
                            if (!pageReader.isNull(column)) {
                                Timestamp value = pageReader.getTimestamp(column);
                                encoder.addText(timestampFormatters.get(column.getIndex()).format(value));
                            }
                        }

                        private void addDelimiter(Column column)
                        {
                            if (column.getIndex() != 0) {
                                encoder.addText(",");
                            }
                        }
                    });

                    encoder.addNewLine();
                }
                page.release();
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

    private void writeHeader(Schema schema, LineEncoder encoder)
    {
        for (Column column : schema.getColumns()) {
            if (column.getIndex() != 0) {
                encoder.addText(",");
            }
            encoder.addText(column.getName());
        }
        encoder.addNewLine();
    }
}
