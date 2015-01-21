package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;

public interface FileOutputPlugin
{
    public interface Control
    {
        public List<CommitReport> run(TaskSource taskSource);
    }

    public NextConfig transaction(ConfigSource config, int processorCount,
            FileOutputPlugin.Control control);

    public TransactionalFileOutput open(TaskSource taskSource, int processorIndex);
}
