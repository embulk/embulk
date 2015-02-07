package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.CommitReport;

public interface FileInputPlugin
{
    public interface Control
    {
        public List<CommitReport> run(TaskSource taskSource,
                int processorCount);
    }

    public ConfigDiff transaction(ConfigSource config,
            FileInputPlugin.Control control);

    public ConfigDiff resume(TaskSource taskSource,
            int processorCount,
            FileInputPlugin.Control control);

    public void cleanup(TaskSource taskSource,
            int processorCount,
            List<CommitReport> successCommitReports);

    public TransactionalFileInput open(TaskSource taskSource,
            int processorIndex);
}
