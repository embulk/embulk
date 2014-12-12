package org.embulk.standards;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.record.Column;
import org.embulk.record.PageReader;
import org.embulk.record.RecordReader;
import org.embulk.record.Schema;
import org.embulk.record.TimestampType;
import org.embulk.time.Timestamp;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.channel.PageInput;
import org.embulk.channel.FileBufferOutput;
import org.embulk.spi.BasicFormatterPlugin;
import org.embulk.spi.ExecTask;
import org.embulk.spi.LineEncoder;
import org.embulk.spi.LineEncoderTask;
import org.embulk.time.TimestampFormatter;
import org.embulk.time.TimestampFormatterTask;

import java.util.Map;

public class CsvFormatterPlugin
        extends BasicFormatterPlugin
{
    public interface CsvFormatterTask
            extends LineEncoderTask, TimestampFormatterTask
    {
        @Config("header_line")
        @ConfigDefault("true")
        public boolean getHeaderLine();
    }

    @Override
    public TaskSource getBasicFormatterTask(ExecTask exec, ConfigSource config)
    {
        CsvFormatterTask task = exec.loadConfig(config, CsvFormatterTask.class);
        return exec.dumpTask(task);
    }

    private Map<Integer, TimestampFormatter> newTimestampFormatters(
            final ExecTask exec, final TimestampFormatterTask task, final Schema schema)
    {
        ImmutableMap.Builder<Integer, TimestampFormatter> builder =
                new ImmutableBiMap.Builder<>();
        for (Column column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                TimestampType tt = (TimestampType) column.getType();
                builder.put(column.getIndex(), exec.newTimestampFormatter(
                        tt.getFormat(), task.getTimeZone()));
            }
        }
        return builder.build();
    }

    @Override
    public void runBasicFormatter(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput)
    {
        final CsvFormatterTask task = exec.loadTask(taskSource, CsvFormatterTask.class);
        final Schema schema = exec.getSchema();
        final LineEncoder encoder = new LineEncoder(
                exec.getBufferAllocator(), task, fileBufferOutput);
        final Map<Integer, TimestampFormatter> timestampFormatters =
                newTimestampFormatters(exec, task, schema);

        if (task.getHeaderLine()) {
            // write header
            writeHeader(schema, encoder);
        }

        try (PageReader reader = new PageReader(exec.getSchema(), pageInput)) {
            while (reader.nextRecord()) {
                reader.visitColumns(new RecordReader() {
                    public void readNull(Column column)
                    {
                        addDelimiter(column);
                    }

                    public void readBoolean(Column column, boolean value)
                    {
                        addDelimiter(column);
                        encoder.addText(Boolean.toString(value));
                    }

                    public void readLong(Column column, long value)
                    {
                        addDelimiter(column);
                        encoder.addText(Long.toString(value));
                    }

                    public void readDouble(Column column, double value)
                    {
                        addDelimiter(column);
                        encoder.addText(Double.toString(value));
                    }

                    public void readString(Column column, String value)
                    {
                        addDelimiter(column);
                        encoder.addText(value);
                    }

                    public void readTimestamp(Column column, Timestamp value)
                    {
                        addDelimiter(column);
                        encoder.addText(timestampFormatters.get(column.getIndex()).format(value));
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
        }
        encoder.flush();
        fileBufferOutput.addFile();
    }

    private void writeHeader(final Schema schema, final LineEncoder encoder)
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
