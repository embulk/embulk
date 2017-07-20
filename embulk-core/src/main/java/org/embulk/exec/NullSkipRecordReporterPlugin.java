package org.embulk.exec;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.SkipRecordReporter;
import org.embulk.spi.SkipRecordReporterPlugin;

import javax.annotation.concurrent.ThreadSafe;

public class NullSkipRecordReporterPlugin
        implements SkipRecordReporterPlugin
{
    @Override
    public TaskSource configureTaskSource(final ConfigSource config)
    {
        return config.loadConfig(Task.class).dump();
    }

    @Override
    public SkipRecordReporter open(final TaskSource task)
    {
        return new NullSkipRecordReporter();
    }

    @ThreadSafe
    public static class NullSkipRecordReporter
            implements SkipRecordReporter
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
