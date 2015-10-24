package org.embulk.spi;

import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.TaskReport;

public interface ErrorPlugin
{
    public static interface Control
    {
        List<TaskReport> run(TaskSource taskSource);
    }

    void transaction(ConfigSource config, ErrorPlugin.Control control);

    TransactionalValueOutput open(TaskSource taskSource);
}
