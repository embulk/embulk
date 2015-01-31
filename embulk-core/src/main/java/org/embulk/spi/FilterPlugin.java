package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;

public interface FilterPlugin
{
    public interface Control
    {
        public List<CommitReport> run(TaskSource taskSource, Schema outputSchema);
    }

    public NextConfig transaction(ConfigSource config, Schema inputSchema,
            FilterPlugin.Control control);

    public TransactionalPageOutput open(TaskSource taskSource, Schema inputSchema,
            Schema outputSchema, TransactionalPageOutput output);
}
