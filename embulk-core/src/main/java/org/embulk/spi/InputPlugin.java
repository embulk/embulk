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
                Schema schema, int taskCount);
    }

    public ConfigDiff transaction(ConfigSource config,
            InputPlugin.Control control);

    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            InputPlugin.Control control);

    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<CommitReport> successCommitReports);

    public CommitReport run(TaskSource taskSource,
            Schema schema, int taskIndex,
            PageOutput output);
}
