package org.quickload.spi;

import org.quickload.config.ConfigSource;

public interface InputPlugin
{
    public InputTransaction newInputTransaction(ConfigSource config);

    public InputProcessor startInputProcessor(InputTask task, int processorIndex, OutputOperator op);

    public void shutdown();
}
