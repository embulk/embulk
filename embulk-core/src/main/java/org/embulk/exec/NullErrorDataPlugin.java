package org.embulk.exec;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.ErrorDataPlugin;
import org.embulk.spi.ErrorDataReporter;

import javax.annotation.concurrent.ThreadSafe;

public class NullErrorDataPlugin
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
        return new NullErrorDataReporter();
    }

    @ThreadSafe
    public static class NullErrorDataReporter
            implements ErrorDataReporter
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
        public void commit()
        {
        }
    }
}
