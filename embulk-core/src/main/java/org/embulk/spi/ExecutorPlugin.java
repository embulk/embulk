package org.embulk.spi;

import org.embulk.config.ConfigSource;

public interface ExecutorPlugin
{
    interface Executor
    {
        void execute(ProcessTask task, ProcessState state);
    }

    interface Control
    {
        void transaction(Schema executorSchema, int outputTaskCount, Executor executor);
    }

    void transaction(ConfigSource config, Schema outputSchema, int inputTaskCount,
            ExecutorPlugin.Control control);
}
