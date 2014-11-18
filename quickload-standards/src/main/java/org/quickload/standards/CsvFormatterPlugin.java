package org.quickload.standards;

import java.sql.Timestamp;
import org.quickload.buffer.Buffer;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.record.Column;
import org.quickload.record.Page;
import org.quickload.record.PageReader;
import org.quickload.record.RecordReader;
import org.quickload.record.Schema;
import org.quickload.channel.PageInput;
import org.quickload.channel.FileBufferOutput;
import org.quickload.spi.BasicFormatterPlugin;
import org.quickload.spi.ExecTask;
import org.quickload.spi.LineEncoder;
import org.quickload.spi.LineEncoderTask;

public class CsvFormatterPlugin
        extends BasicFormatterPlugin
{
    public interface PluginTask
            extends LineEncoderTask
    {
    }

    @Override
    public TaskSource getBasicFormatterTask(ExecTask exec, ConfigSource config)
    {
        PluginTask task = exec.loadConfig(config, PluginTask.class);
        return exec.dumpTask(task);
    }

    @Override
    public void runBasicFormatter(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput)
    {
        PluginTask task = exec.loadTask(taskSource, PluginTask.class);
        final LineEncoder encoder = new LineEncoder(exec.getBufferAllocator(), task, fileBufferOutput);

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
                        encoder.addText(value.toString());  // TODO fromatting timestamp needs timezone
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
}
