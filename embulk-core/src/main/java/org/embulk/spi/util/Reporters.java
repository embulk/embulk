package org.embulk.spi.util;

import com.google.common.collect.ImmutableMap;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.EventLogReporter;
import org.embulk.spi.Reporter;
import org.embulk.spi.ReporterPlugin;
import org.embulk.spi.SkipRecordReporter;

import java.util.Map;

public final class Reporters
        implements AutoCloseable
{
    public static Map<String, ConfigSource> extractConfigSources(final ConfigSource reportersConfig)
    {
        final ImmutableMap.Builder<String, ConfigSource> builder = ImmutableMap.builder();

        for (final Reporter.ReportType type : Reporter.ReportType.values()) {
            final String reportType = type.getType();
            if (reportersConfig.has(reportType)) { // TODO nullable?
                final ConfigSource config = reportersConfig.getNested(reportType);
                builder.put(reportType, config);
            }
        }

        return builder.build();
    }

    public static void transaction(final Map<String, ReporterPlugin> plugins, final Map<String, ConfigSource> configs, final Control control)
    {
        final ImmutableMap.Builder<String, TaskSource> builder = ImmutableMap.builder();

        for (final Reporter.ReportType type : Reporter.ReportType.values()) {
            final String typeName = type.getType();
            final ConfigSource config = configs.get(typeName);
            final TaskSource task = configureTaskSource(plugins.get(typeName), config);
            builder.put(typeName, task);
        }

        control.run(builder.build());
    }

    private static TaskSource configureTaskSource(final ReporterPlugin plugin, final ConfigSource config)
    {
        return plugin.configureTaskSource(config);
    }

    public static Reporters createReporters(final Map<String, ReporterPlugin> plugins, final Map<String, TaskSource> tasks)
    {
        final Reporter skipRecordReporter = plugins.get(Reporter.ReportType.SKIP_RECORD.getType()).open(
                tasks.get(Reporter.ReportType.SKIP_RECORD.getType()));
        final Reporter eventLogReporter = plugins.get(Reporter.ReportType.EVENT_LOG.getType()).open(
                tasks.get(Reporter.ReportType.EVENT_LOG.getType()));

        // TODO add? exception?

        return new Reporters(
                (SkipRecordReporter) skipRecordReporter,
                (EventLogReporter) eventLogReporter);
    }

    public interface Control
    {
        void run(final Map<String, TaskSource> reporterTasks);
    }

    private final SkipRecordReporter skipRecordReporter;
    private final EventLogReporter eventLogReporter;

    private Reporters(
            final SkipRecordReporter skipRecordReporter,
            final EventLogReporter eventLogReporter)
    {
        this.skipRecordReporter = skipRecordReporter;
        this.eventLogReporter = eventLogReporter;
    }

    public void skip(String skipped)
    {
        this.skipRecordReporter.skip(skipped);
    }

    public void log(Reporter.ReportLevel level, String eventLog)
    {
        this.eventLogReporter.log(level, eventLog);
    }

    public void close()
    {
        this.skipRecordReporter.close();
        this.eventLogReporter.close();
        // TODO add? exception?
    }

    public void cleanup()
    {
        this.skipRecordReporter.cleanup();
        this.eventLogReporter.cleanup();
        // TODO add? exception?
    }
}
