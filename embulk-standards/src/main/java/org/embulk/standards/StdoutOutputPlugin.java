package org.embulk.standards;

import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskReport;
import org.embulk.config.Task;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.Schema;
import org.embulk.spi.Page;
import org.embulk.spi.Exec;
import org.embulk.spi.Buffer;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.TransactionalFileOutput;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;

public class StdoutOutputPlugin
        implements FileOutputPlugin
{
    public interface PluginTask
            extends Task, TimestampFormatter.FormatterTask
    {
    }

    @Override
    public ConfigDiff transaction(ConfigSource config,
            int taskCount,
            FileOutputPlugin.Control control)
    {
        final PluginTask task = config.loadConfig(PluginTask.class);
        return resume(task.dump(), taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            int taskCount,
            FileOutputPlugin.Control control)
    {
        control.run(taskSource);
        return Exec.newConfigDiff();
    }

    public void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports)
    { }

    @Override
    public TransactionalFileOutput open(TaskSource taskSource, int taskIndex)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);

        return new TransactionalFileOutput() {
            public void nextFile() {}

            public void closeFile() {}

            public void add(Buffer buffer)
            {
                try {
                    System.out.write(buffer.array(), buffer.offset(), buffer.limit());
                } finally {
                    buffer.release();
                }
            }

            public void finish()
            {
                System.out.flush();
            }

            public void close() { }

            public void abort() { }

            public TaskReport commit()
            {
                return Exec.newTaskReport();
            }
        };
    }
}
