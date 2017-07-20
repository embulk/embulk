package org.embulk.exec;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.ReporterCloseable;
import org.embulk.spi.ReporterPlugin;
import org.embulk.spi.util.Reporters;

import javax.annotation.concurrent.ThreadSafe;

import java.util.Map;

public class NullReporterPlugin
        implements ReporterPlugin
{
    @Override
    public TaskSource configureTaskSource(final ConfigSource config)
    {
        return config.loadConfig(Task.class).dump();
    }

    @Override
    public ReporterCloseable open(final TaskSource task)
    {
        return new NullReporter();
    }

    @ThreadSafe
    public static class NullReporter
            implements ReporterCloseable
    {
        @Override
        public void report(Reporters.ReportLevel level, Map<String, Object> event)
        { }

        @Override
        public void close()
        { }

        @Override
        public void cleanup()
        { }
    }
}