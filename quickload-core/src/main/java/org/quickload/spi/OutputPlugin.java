package org.quickload.spi;

import org.quickload.config.ConfigSource;

public interface OutputPlugin
{
    public OutputTransaction newOutputTransaction(ConfigSource config);

    public OutputOperator openOperator(OutputTask task, int processorIndex);

    public void shutdown();
}
