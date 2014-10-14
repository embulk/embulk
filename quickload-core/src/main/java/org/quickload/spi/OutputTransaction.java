package org.quickload.spi;

import java.util.List;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;

public interface OutputTransaction
{
    public TaskSource getOutputTask(ProcTask proc, ConfigSource config);

    public void begin();

    public void commit(List<Report> reports);

    public void abort(/* TODO */);
}
