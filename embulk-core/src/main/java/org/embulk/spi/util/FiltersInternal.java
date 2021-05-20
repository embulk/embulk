package org.embulk.spi.util;

import java.util.ArrayList;
import java.util.Collections;
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
        final ArrayList<PluginType> builder = new ArrayList<>();
        for (ConfigSource config : configs) {
            builder.add(config.get(PluginType.class, "type"));
        }
        return Collections.unmodifiableList(builder);
    }

    public static List<FilterPlugin> newFilterPluginsFromConfigSources(ExecSessionInternal exec, List<ConfigSource> configs) {
        return newFilterPlugins(exec, getPluginTypes(configs));
    }

    public static List<FilterPlugin> newFilterPlugins(ExecSessionInternal exec, List<PluginType> pluginTypes) {
        final ArrayList<FilterPlugin> builder = new ArrayList<>();
        for (PluginType pluginType : pluginTypes) {
            builder.add(exec.newPlugin(FilterPlugin.class, pluginType));
        }
        return Collections.unmodifiableList(builder);
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
        private final ArrayList<TaskSource> taskSources;
        private final ArrayList<Schema> filterSchemas;
        private int pos;

        RecursiveControl(List<FilterPlugin> plugins, List<ConfigSource> configs,
                FiltersInternal.Control finalControl) {
            this.plugins = plugins;
            this.configs = configs;
            this.finalControl = finalControl;
            this.taskSources = new ArrayList<>();
            this.filterSchemas = new ArrayList<>();
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
                finalControl.run(Collections.unmodifiableList(this.taskSources), Collections.unmodifiableList(this.filterSchemas));
            }
        }
    }
}
