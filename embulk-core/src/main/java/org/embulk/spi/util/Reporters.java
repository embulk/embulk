package org.embulk.spi.util;

import com.google.common.collect.ImmutableMap;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.Reporter;
import org.embulk.spi.ReporterPlugin;

import java.util.Map;

public final class Reporters
{
    public enum Type
    {
        SKIP_RECORD("skip_record");

        private final String typeName;

        Type(final String typeName)
        {
            this.typeName = typeName;
        }

        public String getTypeName()
        {
            return this.typeName;
        }
    }

    public static Map<String, ConfigSource> extractConfigSources(final ConfigSource reportersConfig)
    {
        final ImmutableMap.Builder<String, ConfigSource> builder = ImmutableMap.builder();

        for (final Type type : Type.values()) {
            if (reportersConfig.has(type.getTypeName())) {
                final ConfigSource config = reportersConfig.getNested(type.getTypeName());
                builder.put(type.getTypeName(), config);
            }
            // TODO
        }

        return builder.build();
    }

    public static void transaction(final Map<String, ReporterPlugin> plugins, final Map<String, ConfigSource> configs, final Control control)
    {
        final ImmutableMap.Builder<String, TaskSource> builder = ImmutableMap.builder();

        for (final Type type : Type.values()) {
            final ConfigSource config = configs.get(type.getTypeName());
            final TaskSource task = configureTaskSource(plugins.get(type.getTypeName()), config);
            builder.put(type.getTypeName(), task);
            // TODO
        }

        control.run(builder.build());
    }

    private static TaskSource configureTaskSource(final ReporterPlugin plugin, final ConfigSource config)
    {
        return plugin.configureTaskSource(config);
    }

    public static Reporter open(final Map<String, ReporterPlugin> plugins, final Map<String, TaskSource> tasks)
    {
        final Reporter skipRecordReporter = plugins.get(Type.SKIP_RECORD.getTypeName()).open(tasks.get(Type.SKIP_RECORD.getTypeName()));

        // TODO

        return new ReporterWrapper(skipRecordReporter);
    }

    public interface Control
    {
        void run(final Map<String, TaskSource> reporterTasks);
    }

    public static final class ReporterWrapper
            implements Reporter
    {
        private final Reporter skipRecordReporter;

        private ReporterWrapper(final Reporter skipRecordReporter)
        {
            this.skipRecordReporter = skipRecordReporter;
        }

        @Override
        public void skip(String skipped)
        {
            this.skipRecordReporter.skip(skipped);
        }

        @Override
        public void close()
        {
            this.skipRecordReporter.close();
            // TODO
        }

        @Override
        public void cleanup()
        {
            this.skipRecordReporter.cleanup();
            // TODO
        }
    }
}
