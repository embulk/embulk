package org.embulk.spi;

import org.embulk.config.ConfigSource;

public interface ExecutorPlugin
{
    public interface Executor
    {
        public void execute(ProcessTask task, ProcessState state);
    }

    public interface Control
    {
        public void transaction(Schema executorSchema, int outputTaskCount, Executor executor);
    }

    public void transaction(ConfigSource config, Schema outputSchema, int inputTaskCount,
            ExecutorPlugin.Control control);
}
