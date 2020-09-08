package org.embulk.spi.util;

import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;

/**
 * Utility class for handling multiple filter plugins.
 *
 * <p>It is considered to be an internal class, not for plugins. To make it explicit, {@link FiltersInternal} replaces it.
 */
@Deprecated
public abstract class Filters {
    private Filters() {}

    public static List<PluginType> getPluginTypes(List<ConfigSource> configs) {
        return FiltersInternal.getPluginTypes(configs);
    }

    public static List<FilterPlugin> newFilterPluginsFromConfigSources(ExecSession exec, List<ConfigSource> configs) {
        if (!(exec instanceof ExecSessionInternal)) {
            throw new IllegalArgumentException(new ClassCastException());
        }
        final ExecSessionInternal execInternal = (ExecSessionInternal) exec;

        return FiltersInternal.newFilterPluginsFromConfigSources(execInternal, configs);
    }

    public static List<FilterPlugin> newFilterPlugins(ExecSession exec, List<PluginType> pluginTypes) {
        if (!(exec instanceof ExecSessionInternal)) {
            throw new IllegalArgumentException(new ClassCastException());
        }
        final ExecSessionInternal execInternal = (ExecSessionInternal) exec;

        return FiltersInternal.newFilterPlugins(execInternal, pluginTypes);
    }

    public interface Control extends FiltersInternal.Control {
        @Override
        public void run(List<TaskSource> taskSources, List<Schema> filterSchemas);
    }

    public static void transaction(List<FilterPlugin> plugins, List<ConfigSource> configs,
            Schema inputSchema, Filters.Control control) {
        FiltersInternal.transaction(plugins, configs, inputSchema, control);
    }

    public static PageOutput open(List<FilterPlugin> plugins, List<TaskSource> taskSources,
            List<Schema> filterSchemas, PageOutput output) {
        return FiltersInternal.open(plugins, taskSources, filterSchemas, output);
    }
}
