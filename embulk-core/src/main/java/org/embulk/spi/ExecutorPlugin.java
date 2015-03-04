package org.embulk.spi;

import org.embulk.config.ConfigSource;

public interface ExecutorPlugin
{
    public interface Executor
    {
        public void execute(ProcessTask task, int taskCount, ProcessState state);
    }

    public interface Control
    {
        public void transaction(Executor executor);
    }

    public void transaction(ConfigSource config, ExecutorPlugin.Control control);
}
