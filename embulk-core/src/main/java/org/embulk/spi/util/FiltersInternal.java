package org.embulk.spi.util;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;

public abstract class FiltersInternal {
    private FiltersInternal() {}

    public static List<PluginType> getPluginTypes(List<ConfigSource> configs) {
        ImmutableList.Builder<PluginType> builder = ImmutableList.builder();
        for (ConfigSource config : configs) {
            builder.add(config.get(PluginType.class, "type"));
        }
        return builder.build();
    }

    public static List<FilterPlugin> newFilterPluginsFromConfigSources(ExecSessionInternal exec, List<ConfigSource> configs) {
        return newFilterPlugins(exec, getPluginTypes(configs));
    }

    public static List<FilterPlugin> newFilterPlugins(ExecSessionInternal exec, List<PluginType> pluginTypes) {
        ImmutableList.Builder<FilterPlugin> builder = ImmutableList.builder();
        for (PluginType pluginType : pluginTypes) {
            builder.add(exec.newPlugin(FilterPlugin.class, pluginType));
        }
        return builder.build();
    }

    public interface Control {
        public void run(List<TaskSource> taskSources, List<Schema> filterSchemas);
    }

    public static void transaction(List<FilterPlugin> plugins, List<ConfigSource> configs,
            Schema inputSchema, FiltersInternal.Control control) {
        new RecursiveControl(plugins, configs, control).transaction(inputSchema);
    }

    public static PageOutput open(List<FilterPlugin> plugins, List<TaskSource> taskSources,
            List<Schema> filterSchemas, PageOutput output) {
        PageOutput out = output;
        int pos = plugins.size() - 1;
        while (pos >= 0) {
            out = plugins.get(pos).open(taskSources.get(pos), filterSchemas.get(pos), filterSchemas.get(pos + 1), out);
            pos--;
        }
        return out;
    }

    private static class RecursiveControl {
        private final List<FilterPlugin> plugins;
        private final List<ConfigSource> configs;
        private final FiltersInternal.Control finalControl;
        private final ImmutableList.Builder<TaskSource> taskSources;
        private final ImmutableList.Builder<Schema> filterSchemas;
        private int pos;

        RecursiveControl(List<FilterPlugin> plugins, List<ConfigSource> configs,
                FiltersInternal.Control finalControl) {
            this.plugins = plugins;
            this.configs = configs;
            this.finalControl = finalControl;
            this.taskSources = ImmutableList.builder();
            this.filterSchemas = ImmutableList.builder();
        }

        public void transaction(Schema inputSchema) {
            filterSchemas.add(inputSchema);
            if (pos < plugins.size()) {
                plugins.get(pos).transaction(configs.get(pos), inputSchema, new FilterPlugin.Control() {
                        public void run(TaskSource taskSource, Schema outputSchema) {
                            taskSources.add(taskSource);
                            pos++;
                            transaction(outputSchema);
                        }
                    });
            } else {
                finalControl.run(taskSources.build(), filterSchemas.build());
            }
        }
    }
}
