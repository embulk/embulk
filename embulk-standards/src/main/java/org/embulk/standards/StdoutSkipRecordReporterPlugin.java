package org.embulk.standards;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.SkipRecordReporter;
import org.embulk.spi.SkipRecordReporterPlugin;

import javax.annotation.concurrent.ThreadSafe;

public class StdoutSkipRecordReporterPlugin
        implements SkipRecordReporterPlugin
{
    public TaskSource configureTaskSource(final ConfigSource config)
    {
        return config.loadConfig(Task.class).dump();
    }

    public SkipRecordReporter open(final TaskSource task)
    {
        return new StdoutSkipRecordReporter();
    }

    @ThreadSafe
    private static class StdoutSkipRecordReporter
            implements SkipRecordReporter
    {
        @Override
        public void skip(String skipped)
        {
            System.out.println("StdoutReporter#skip");
            System.out.println(skipped);
        }

        @Override
        public void close()
        {
            System.out.println("StdoutReporter#close");
        }

        @Override
        public void cleanup()
        {
            System.out.println("StdoutReporter#cleanup");
        }
    }
}
