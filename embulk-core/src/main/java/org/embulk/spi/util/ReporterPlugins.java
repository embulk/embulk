package org.embulk.spi.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.Reporter;
import org.embulk.spi.ReporterPlugin;

import java.util.Map;

public class ReporterPlugins
{
    public static Map<Reporters.Type, ConfigSource> extractConfigSources(final ConfigSource reportersConfig)
    {
        final ImmutableMap.Builder<Reporters.Type, ConfigSource> builder = ImmutableMap.builder();
        for (final Reporters.Type type : Reporters.Type.values()) {
            final String typeName = type.toString();
            final ConfigSource config = reportersConfig.getNestedOrGetEmpty(typeName);
            // even though the type doesn't appear in reporters config, empty Json object is stored.
            builder.put(type, config);
        }
        return Maps.immutableEnumMap(builder.build());
    }

    public static void transaction(
            final Map<Reporters.Type, ReporterPlugin> plugins,
            final Map<Reporters.Type, ConfigSource> configs,
            final Control control)
    {
        final ImmutableMap.Builder<Reporters.Type, TaskSource> builder = ImmutableMap.builder();
        for (final Reporters.Type type : Reporters.Type.values()) {
            final ConfigSource config = configs.get(type);
            final ReporterPlugin reporterPlugin = plugins.get(type);
            final TaskSource task = reporterPlugin.configureTaskSource(config);
            builder.put(type, task);
        }
        control.run(Maps.immutableEnumMap(builder.build()));
    }

    public static Reporters createReporters(
            final Map<Reporters.Type, ReporterPlugin> plugins,
            final Map<Reporters.Type, TaskSource> tasks)
    {
        final ImmutableMap.Builder<Reporters.Type, Reporter> builder = ImmutableMap.builder();
        for (final Reporters.Type type : Reporters.Type.values()) {
            final ReporterPlugin plugin = plugins.get(type);
            final TaskSource task = tasks.get(type);
            builder.put(type, plugin.open(task));
        }
        return new Reporters(Maps.immutableEnumMap(builder.build()));
    }

    public interface Control
    {
        void run(final Map<Reporters.Type, TaskSource> reporterTasks);
    }
}
