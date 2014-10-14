package org.quickload.spi;

import org.quickload.config.ConfigSource;
import org.quickload.config.TaskSource;
import java.util.List;

public interface InputTransaction
{
    public TaskSource getInputTask(ProcConfig proc, ConfigSource config);

    public void begin();

    public void commit(List<Report> reports);

    public void abort(/* TODO */);
}
