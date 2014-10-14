package org.quickload.standards;

import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.record.Page;
import org.quickload.record.Schema;
import org.quickload.spi.*;

public class MessagePackFormatterPlugin
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
    public PageOperator openPageOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex, BufferOperator next)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        return new PluginOperator(proc.getSchema(), processorIndex, next);
    }

    @Override
    public void shutdown()
    {
        // TODO
    }

    class PluginOperator
            extends AbstractOperator<BufferOperator>
            implements PageOperator
    {
        private final Schema schema;
        private final int processorIndex;

        private PluginOperator(Schema schema, int processorIndex, BufferOperator next)
        {
            super(next);
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
