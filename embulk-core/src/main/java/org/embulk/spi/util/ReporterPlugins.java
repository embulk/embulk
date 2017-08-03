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
    public static Map<Reporter.Channel, ConfigSource> extractConfigSources(final ConfigSource reportersConfig)
    {
        final ImmutableMap.Builder<Reporter.Channel, ConfigSource> builder = ImmutableMap.builder();
        for (final Reporter.Channel channel : Reporter.Channel.values()) {
            final String typeName = channel.toString();
            final ConfigSource config = reportersConfig.getNestedOrGetEmpty(typeName);
            // even though the channel doesn't appear in reporters config, empty Json object is stored.
            builder.put(channel, config);
        }
        return Maps.immutableEnumMap(builder.build());
    }

    public static void transaction(
            final Map<Reporter.Channel, ReporterPlugin> plugins,
            final Map<Reporter.Channel, ConfigSource> configs,
            final Control control)
    {
        final ImmutableMap.Builder<Reporter.Channel, TaskSource> builder = ImmutableMap.builder();
        for (final Reporter.Channel channel : Reporter.Channel.values()) {
            final ConfigSource config = configs.get(channel);
            final ReporterPlugin reporterPlugin = plugins.get(channel);
            final TaskSource task = reporterPlugin.configureTaskSource(config);
            builder.put(channel, task);
        }
        control.run(Maps.immutableEnumMap(builder.build()));
    }

    public static Reporters createReporters(
            final Map<Reporter.Channel, ReporterPlugin> plugins,
            final Map<Reporter.Channel, TaskSource> tasks)
    {
        final ImmutableMap.Builder<Reporter.Channel, Reporter> builder = ImmutableMap.builder();
        for (final Reporter.Channel channel : Reporter.Channel.values()) {
            final ReporterPlugin plugin = plugins.get(channel);
            final TaskSource task = tasks.get(channel);
            builder.put(channel, plugin.open(task));
        }
        return new Reporters(Maps.immutableEnumMap(builder.build()));
    }

    public interface Control
    {
        void run(final Map<Reporter.Channel, TaskSource> reporterTasks);
    }
}
