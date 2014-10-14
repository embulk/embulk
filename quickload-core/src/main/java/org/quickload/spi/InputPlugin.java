package org.quickload.spi;

import org.quickload.config.TaskSource;

public interface InputPlugin
{
    public InputTransaction newInputTransaction();

    public InputProcessor startInputProcessor(ProcTask proc,
            TaskSource taskSource, int processorIndex, PageOperator next);

    public void shutdown();
}
