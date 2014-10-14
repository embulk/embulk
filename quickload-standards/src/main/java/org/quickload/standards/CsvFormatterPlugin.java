package org.quickload.standards;

import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.exec.BufferManager;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.record.*;
import org.quickload.spi.*;

public class CsvFormatterPlugin
        implements FormatterPlugin
{
    private final BufferManager bufferManager;

    @Inject
    public CsvFormatterPlugin(BufferManager bufferManager)
    {
        this.bufferManager = bufferManager;
    }

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
    public PageOperator openPageOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex, BufferOperator next)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        return new PluginOperator(proc.getSchema(), processorIndex, next);
    }

    public void shutdown()
    {
        // TODO
    }

    class PluginOperator
            extends AbstractOperator<BufferOperator>
            implements PageOperator
    {
        private final Schema schema;
        private final PageReader pageReader;
        private final int processorIndex;

        private PluginOperator(Schema schema, int processorIndex, BufferOperator next)
        {
            super(next);
            this.schema = schema;
            this.pageReader = new PageReader(bufferManager, schema);
            this.processorIndex = processorIndex;
        }

        @Override
        public void addPage(Page page)
        {
            // TODO simple implementation
            final StringBuilder sbuf = new StringBuilder(); // TODO

            RecordCursor recordCursor = pageReader.cursor(page);

            while (recordCursor.next()) {
                RecordConsumer recordConsumer = new RecordConsumer()
                {
                    @Override
                    public void setNull(Column column) {
                        sbuf.append(',');
                    }

                    @Override
                    public void setLong(Column column, long value) {
                        sbuf.append(Long.toString(value)).append(',');
                    }

                    @Override
                    public void setDouble(Column column, double value) {
                        sbuf.append(Double.toString(value)).append(',');
                    }

                    @Override
                    public void setString(Column column, String value) {
                        sbuf.append(value).append(',');
                    }
                };
                schema.consume(recordCursor, recordConsumer);
                sbuf.append('\n');
            }
            next.addBuffer(new Buffer(sbuf.toString().getBytes()));
        }

        @Override
        public Report completed()
        {
            return null; // TODO
        }

        @Override
        public void close()
        {
            // TODO
        }
    }
}
