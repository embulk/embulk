package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;
import org.embulk.type.Schema;

public interface InputPlugin
{
    public interface Control
    {
        public List<CommitReport> run(TaskSource taskSource, Schema schema, int processorCount);
    }

    public NextConfig transaction(ConfigSource config, InputPlugin.Control control);

    public CommitReport run(TaskSource taskSource, Schema schema, int processorIndex,
            PageOutput output);
}
