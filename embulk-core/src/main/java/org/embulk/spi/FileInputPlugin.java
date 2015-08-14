package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskReport;

public interface FileInputPlugin
{
    interface Control
    {
        List<TaskReport> run(TaskSource taskSource,
                int taskCount);
    }

    ConfigDiff transaction(ConfigSource config,
            FileInputPlugin.Control control);

    ConfigDiff resume(TaskSource taskSource,
            int taskCount,
            FileInputPlugin.Control control);

    void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports);

    TransactionalFileInput open(TaskSource taskSource,
            int taskIndex);
}
