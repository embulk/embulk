package org.quickload.standards;

import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.exec.BufferManager;
import org.quickload.config.ConfigSource;
import org.quickload.record.*;
import org.quickload.spi.*;

public class CsvFormatterPlugin
        extends BasicFormatterPlugin<CsvFormatterPlugin.Task>
{
    private final BufferManager bufferManager;

    @Inject
    public CsvFormatterPlugin(BufferManager bufferManager)
    {
        this.bufferManager = bufferManager;
    }

    public interface Task
            extends FormatterTask
    {
        public void setSchema(Schema schema);
    }

    @Override
    public Task getTask(ConfigSource source, InputTask input)
    {
        Task task = source.load(Task.class);
        task.setSchema(input.getSchema());
        task.validate();
        return task;
    }

    @Override
    public OutputOperator openOperator(Task task, int processorIndex, BufferOperator op)
    {
        return new Operator(task.getSchema(), processorIndex, op);
    }

    public void shutdown()
    {
        // TODO
    }

    class Operator
            extends AbstractOperator<BufferOperator>
            implements OutputOperator
    {
        private final Schema schema;
        private final PageReader pageReader;
        private final int processorIndex;

        private Operator(Schema schema, int processorIndex, BufferOperator op)
        {
            super(op);
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
