package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.CommitReport;

public interface InputPlugin
{
    public interface Control
    {
        public List<CommitReport> run(TaskSource taskSource,
                Schema schema, int processorCount);
    }

    public ConfigDiff transaction(ConfigSource config,
            InputPlugin.Control control);

    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int processorCount,
            InputPlugin.Control control);

    public void cleanup(TaskSource taskSource,
            Schema schema, int processorCount,
            List<CommitReport> successCommitReports);

    public CommitReport run(TaskSource taskSource,
            Schema schema, int processorIndex,
            PageOutput output);
}
