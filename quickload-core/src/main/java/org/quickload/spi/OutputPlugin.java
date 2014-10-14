package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public interface OutputPlugin
{
    public OutputTransaction newOutputTransaction(ConfigSource config);

    public OutputOperator openOutputOperator(TaskSource taskSource, int processorIndex);

    public void shutdown();
}
