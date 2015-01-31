package org.embulk.spi.util;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.CommitReport;
import org.embulk.config.NextConfig;
import org.embulk.plugin.PluginType;
import org.embulk.spi.ExecSession;
import org.embulk.spi.Schema;
import org.embulk.spi.TransactionalPageOutput;
import org.embulk.spi.FilterPlugin;

public abstract class Filters
{
    private Filters() { }

    public static List<FilterPlugin> newFilterPlugins(ExecSession exec, List<ConfigSource> configs)
    {
        ImmutableList.Builder<FilterPlugin> builder = ImmutableList.builder();
        for (ConfigSource config : configs) {
            builder.add(exec.newPlugin(FilterPlugin.class, config.get(PluginType.class, "type")));
        }
        return builder.build();
    }

    public interface Control
    {
        public List<List<CommitReport>> run(List<TaskSource> taskSources, List<Schema> outputSchemas);
    }

    public static List<NextConfig> transaction(List<FilterPlugin> plugins, List<ConfigSource> configs,
            Schema inputSchema, Filters.Control control)
    {
        RecursiveControl c = new RecursiveControl(plugins, configs, control);
        c.transaction(inputSchema);
        return c.getNextConfigs();
    }

    public static TransactionalPageOutput open(List<FilterPlugin> plugins, List<TaskSource> taskSources,
            Schema inputSchema, List<Schema> outputSchemas, TransactionalPageOutput output)
    {
        TransactionalPageOutput out = output;
        int pos = 0;
        while (pos < plugins.size()) {
            if (pos > 0) {
                inputSchema = outputSchemas.get(pos - 1);
            }
            out = plugins.get(pos).open(taskSources.get(pos), inputSchema, outputSchemas.get(pos), out);
            pos++;
        }
        return out;
    }

    private static class RecursiveControl
    {
        private final List<FilterPlugin> plugins;
        private final List<ConfigSource> configs;
        private final Filters.Control finalControl;
        private final ImmutableList.Builder<TaskSource> taskSources;
        private final ImmutableList.Builder<Schema> outputSchemas;
        private int pos;
        private List<List<CommitReport>> commitReports;
        private final ImmutableList.Builder<NextConfig> nextConfigs;

        RecursiveControl(List<FilterPlugin> plugins, List<ConfigSource> configs,
                Filters.Control finalControl)
        {
            this.plugins = plugins;
            this.configs = configs;
            this.finalControl = finalControl;
            this.taskSources = ImmutableList.builder();
            this.outputSchemas = ImmutableList.builder();
            this.nextConfigs = ImmutableList.builder();
        }

        public void transaction(Schema previousSchema)
        {
            if (pos < plugins.size()) {
                NextConfig nextConfig = plugins.get(pos).transaction(configs.get(pos), previousSchema, new FilterPlugin.Control() {
                    public List<CommitReport> run(TaskSource taskSource, Schema outputSchema)
                    {
                        taskSources.add(taskSource);
                        outputSchemas.add(outputSchema);
                        pos++;
                        transaction(outputSchema);
                        return commitReports.get(pos);
                    }
                });
                nextConfigs.add(nextConfig);
            } else {
                this.commitReports = finalControl.run(taskSources.build(), outputSchemas.build());
            }
        }

        public List<NextConfig> getNextConfigs()
        {
            return nextConfigs.build();
        }
    }
}
