package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskReport;

public interface FileOutputPlugin
{
    interface Control
    {
        List<TaskReport> run(TaskSource taskSource);
    }

    ConfigDiff transaction(ConfigSource config, int taskCount,
            FileOutputPlugin.Control control);

    ConfigDiff resume(TaskSource taskSource,
            int taskCount,
            FileOutputPlugin.Control control);

    void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports);

    TransactionalFileOutput open(TaskSource taskSource, int taskIndex);
}
