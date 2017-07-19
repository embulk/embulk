package org.embulk.exec;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.ErrorDataPlugin;
import org.embulk.spi.ErrorDataReporter;

public class NullErrorDataPlugin
    implements ErrorDataPlugin
{

    @Override
    public TaskSource createTaskSource(ConfigSource configSource)
    {
        return configSource.loadConfig(Task.class).dump();
    }

    @Override
    public ErrorDataReporter open(TaskSource taskSource)
    {
        return null;
    }

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
