package org.embulk.exec;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Reporter;
import org.embulk.spi.ReporterPlugin;

import javax.annotation.concurrent.ThreadSafe;

public class NullReporterPlugin
        implements ReporterPlugin
{
    @Override
    public TaskSource configureTaskSource(final ConfigSource config)
    {
        return config.loadConfig(Task.class).dump();
    }

    @Override
    public Reporter open(final TaskSource task)
    {
        return new NullReporter();
    }

    @ThreadSafe
    public static class NullReporter
            implements Reporter
    {
        @Override
        public void skip(String errorData)
        {
        }

        @Override
        public void close()
        {
        }

        @Override
        public void cleanup()
        {
        }
    }
}
