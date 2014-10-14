package org.quickload.spi;

import java.util.List;
import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;

public abstract class BasicInputPlugin
        implements InputPlugin, InputTransaction
{
    @Override  // InputTransaction
    public abstract TaskSource getInputTask(ProcConfig proc, ConfigSource config);

    @Override  // InputTransaction
    public void begin()
    {
    }

    @Override  // InputTransaction
    public void commit(List<Report> reports)
    {
    }

    @Override  // InputTransaction
    public void abort()
    {
    }

    @Override  // InputPlugin
    public InputTransaction newInputTransaction()
    {
        return this;
    }

    @Override  // InputPlugin
    public abstract InputProcessor startInputProcessor(ProcTask proc,
            TaskSource taskSource, int processorIndex, PageOperator next);

    @Override  // InputPlugin
    public void shutdown()
    {
    }
}
