package org.embulk.standards;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.ErrorDataPlugin;
import org.embulk.spi.ErrorDataReporter;

import javax.annotation.concurrent.ThreadSafe;

public class StdoutErrorDataPlugin
        implements ErrorDataPlugin
{
    @Override
    public TaskSource configureTaskSource(final ConfigSource config)
    {
        return config.loadConfig(Task.class).dump();
    }

    @Override
    public ErrorDataReporter open(final TaskSource task)
    {
        return new StdoutErrorDataReporter();
    }

    @ThreadSafe
    private static class StdoutErrorDataReporter
            implements ErrorDataReporter
    {
        @Override
        public void skip(String skipped)
        {
            System.out.println("StdoutErrorDataReporter#skip");
            System.out.println(skipped);
        }

        @Override
        public void close()
        {
            System.out.println("StdoutErrorDataReporter#close");
        }

        @Override
        public void cleanup()
        {
            System.out.println("StdoutErrorDataReporter#cleanup");
        }
    }
}
