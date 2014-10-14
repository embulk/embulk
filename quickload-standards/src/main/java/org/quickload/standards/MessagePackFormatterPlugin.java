package org.quickload.standards;

import org.quickload.config.ConfigSource;
import org.quickload.record.Page;
import org.quickload.record.Schema;
import org.quickload.spi.*;

public class MessagePackFormatterPlugin
        extends BasicFormatterPlugin<MessagePackFormatterPlugin.Task>
{
    public interface Task
            extends FormatterTask
    {
        public void setSchema(Schema schema);
    }

    @Override
    public Task getTask(ConfigSource config, InputTask input)
    {
        Task task = config.load(Task.class);
        task.setSchema(input.getSchema());
        return task;
    }

    @Override
    public OutputOperator openOperator(Task task, int processorIndex, BufferOperator op)
    {
        return new Operator(task.getSchema(), processorIndex, op);
    }

    @Override
    public void shutdown()
    {
        // TODO
    }

    class Operator
            extends AbstractOperator<BufferOperator>
            implements OutputOperator
    {
        private final Schema schema;
        private final int processorIndex;

        private Operator(Schema schema, int processorIndex, BufferOperator op)
        {
            super(op);
            this.schema = schema;
            this.processorIndex = processorIndex;
        }

        @Override
        public void addPage(Page page)
        {
            // TODO
        }

        @Override
        public Report completed()
        {
            return null; // TODO
        }

        @Override
        public void close() throws Exception
        {
            // TODO
        }
    }
}
