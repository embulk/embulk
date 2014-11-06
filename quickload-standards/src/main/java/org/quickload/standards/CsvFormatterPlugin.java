package org.quickload.standards;

import org.quickload.buffer.Buffer;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.record.Column;
import org.quickload.record.Page;
import org.quickload.record.PageReader;
import org.quickload.record.RecordConsumer;
import org.quickload.record.RecordCursor;
import org.quickload.record.Schema;
import org.quickload.channel.PageInput;
import org.quickload.channel.FileBufferOutput;
import org.quickload.spi.FormatterPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.LineEncoder;
import org.quickload.spi.LineEncoderTask;

public class CsvFormatterPlugin
        implements FormatterPlugin
{
    public interface PluginTask
            extends LineEncoderTask
    {
    }

    @Override
    public TaskSource getFormatterTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);
        return proc.dumpTask(task);
    }

    @Override
    public void runFormatter(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput)
    {
        PluginTask task = proc.loadTask(taskSource, PluginTask.class);

        PageReader pageReader = new PageReader(proc.getSchema());
        Schema schema = proc.getSchema();

        final LineEncoder encoder = new LineEncoder(proc.getBufferAllocator(), task, fileBufferOutput);

        for (Page page : pageInput) {
            try (RecordCursor recordCursor = pageReader.cursor(page)) {
                while (recordCursor.next()) {
                    schema.consume(recordCursor, new RecordConsumer() {
                        public void setNull(Column column)
                        {
                            addDelimiter(column);
                        }

                        public void setLong(Column column, long value)
                        {
                            addDelimiter(column);
                            encoder.addText(Long.toString(value));
                        }

                        public void setDouble(Column column, double value)
                        {
                            addDelimiter(column);
                            encoder.addText(Double.toString(value));
                        }

                        public void setString(Column column, String value)
                        {
                            addDelimiter(column);
                            encoder.addText(value);
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
        }
        fileBufferOutput.addFile();
    }
}
