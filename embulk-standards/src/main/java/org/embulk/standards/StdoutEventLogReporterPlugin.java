package org.embulk.standards;

import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.EventLogReporter;
import org.embulk.spi.EventLogReporterPlugin;
import org.embulk.spi.Reporter;

import javax.annotation.concurrent.ThreadSafe;

public class StdoutEventLogReporterPlugin
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
        return new StdoutEventLogReporter();
    }

    @ThreadSafe
    private static class StdoutEventLogReporter
            implements EventLogReporter
    {
        @Override
        public void log(Reporter.ReportLevel level, String eventLog)
        {
            System.out.println("StdoutEventLogReporter#log");
            System.out.println(eventLog);
        }

        @Override
        public void close()
        {
            System.out.println("StdoutEventLogReporter#close");
        }

        @Override
        public void cleanup()
        {
            System.out.println("StdoutEventLogReporter#cleanup");
        }
    }
}
