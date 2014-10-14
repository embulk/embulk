package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public interface InputPlugin
{
    public InputTransaction newInputTransaction(ConfigSource config);

    public InputProcessor startInputProcessor(TaskSource taskSource, int processorIndex, OutputOperator op);

    public void shutdown();
}
