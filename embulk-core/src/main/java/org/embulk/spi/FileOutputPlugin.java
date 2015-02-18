package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.CommitReport;

public interface FileOutputPlugin
{
    public interface Control
    {
        public List<CommitReport> run(TaskSource taskSource);
    }

    public ConfigDiff transaction(ConfigSource config, int processorCount,
            FileOutputPlugin.Control control);

    public ConfigDiff resume(TaskSource taskSource,
            int processorCount,
            FileOutputPlugin.Control control);

    public void cleanup(TaskSource taskSource,
            int processorCount,
            List<CommitReport> successCommitReports);

    public TransactionalFileOutput open(TaskSource taskSource, int taskIndex);
}
