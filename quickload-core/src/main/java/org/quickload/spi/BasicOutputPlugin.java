package org.quickload.spi;

import java.util.List;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public abstract class BasicOutputPlugin
        implements OutputPlugin, OutputTransaction
{
    @Override  // OutputTransaction
    public abstract TaskSource getOutputTask(ProcTask proc, ConfigSource config);

    @Override  // OutputTransaction
    public void begin()
    {
    }

    @Override  // OutputTransaction
    public void commit(List<Report> reports)
    {
    }

    @Override  // OutputTransaction
    public void abort()
    {
    }

    @Override  // OutputPlugin
    public OutputTransaction newOutputTransaction()
    {
        return this;
    }

    @Override  // OutputPlugin
    public abstract PageOperator openPageOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex);

    @Override  // OutputPlugin
    public void shutdown()
    {
    }
}
