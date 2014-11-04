package org.quickload.standards;

import org.quickload.buffer.Buffer;
import org.quickload.buffer.BufferAllocator;
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

public class CsvFormatterPlugin
        implements FormatterPlugin
{
    public interface PluginTask
            extends Task
    {
    }

    @Override
    public TaskSource getFormatterTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = config.loadTask(PluginTask.class);
        return config.dumpTask(task);
    }

    @Override
    public void runFormatter(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            PageInput pageInput, FileBufferOutput fileBufferOutput)
    {
        PageReader pageReader = new PageReader(proc.getSchema());
        Schema schema = proc.getSchema();
        BufferAllocator bufferAllocator = proc.getBufferAllocator();

        StringBuilder sbuf = new StringBuilder();
        CSVRecordConsumer recordConsumer = new CSVRecordConsumer();
        recordConsumer.setStringBuilder(sbuf); // TODO multiple sbuf

        for (Page page : pageInput) {
            // TODO simple implementation

            RecordCursor recordCursor = pageReader.cursor(page);

            while (recordCursor.next()) {
                schema.consume(recordCursor, recordConsumer);
                sbuf.delete(sbuf.length() - 1, sbuf.length());
                sbuf.append('\n');
            }

            byte[] bytes = sbuf.toString().getBytes();
            Buffer buf = bufferAllocator.allocateBuffer(bytes.length); // TODO
            buf.write(bytes, 0, bytes.length);
            fileBufferOutput.add(buf);
        }
        fileBufferOutput.addFile();
    }

    static class CSVRecordConsumer implements RecordConsumer
    {
        private StringBuilder sbuf;

        CSVRecordConsumer()
        {
        }

        public void setStringBuilder(StringBuilder sbuf)
        {
            this.sbuf = sbuf;
        }

        @Override
        public void setNull(Column column)
        {
            sbuf.append(',');
        }

        @Override
        public void setLong(Column column, long value)
        {
            sbuf.append(Long.toString(value)).append(',');
        }

        @Override
        public void setDouble(Column column, double value)
        {
            sbuf.append(Double.toString(value)).append(',');
        }

        @Override
        public void setString(Column column, String value)
        {
            sbuf.append(value).append(',');
        }
    }
}
