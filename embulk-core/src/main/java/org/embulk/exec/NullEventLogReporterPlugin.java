package org.embulk.exec;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.EventLogReporter;
import org.embulk.spi.EventLogReporterPlugin;
import org.embulk.spi.Reporter;

import javax.annotation.concurrent.ThreadSafe;

public class NullEventLogReporterPlugin
        implements EventLogReporterPlugin
{
    @Override
    public TaskSource configureTaskSource(ConfigSource config)
    {
        return config.loadConfig(Task.class).dump();
    }

    @Override
    public EventLogReporter open(TaskSource errorDataTask)
    {
        return new NullEventLogReporter();
    }

    @ThreadSafe
    private static class NullEventLogReporter
            implements EventLogReporter
    {
        @Override
        public void log(Reporter.ReportLevel level, String eventLog)
        { }

        @Override
        public void close()
        { }

        @Override
        public void cleanup()
        { }
    }
}
