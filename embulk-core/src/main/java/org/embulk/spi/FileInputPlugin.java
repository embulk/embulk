package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;

public interface FileInputPlugin
{
    public interface Control
    {
        public List<CommitReport> run(TaskSource taskSource,
                int processorCount);
    }

    public NextConfig transaction(ConfigSource config,
            FileInputPlugin.Control control);

    public NextConfig resume(TaskSource taskSource,
            int processorCount,
            FileInputPlugin.Control control);

    public void cleanup(TaskSource taskSource,
            int processorCount,
            List<CommitReport> successCommitReports);

    public TransactionalFileInput open(TaskSource taskSource,
            int processorIndex);
}
