package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;
import org.embulk.type.Schema;

public interface OutputPlugin
{
    public interface Control
    {
        public List<CommitReport> run(TaskSource taskSource);
    }

    public NextConfig transaction(ConfigSource config,
            Schema schema, int processorCount,
            OutputPlugin.Control control);

    public TransactionalPageOutput open(TaskSource taskSource, Schema schema, int processorIndex);
}
