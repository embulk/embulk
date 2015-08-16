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
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.TransactionalPageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.util.PagePrinter;

public class StdoutOutputPlugin
        implements OutputPlugin
{
    public interface PluginTask
            extends Task, TimestampFormatter.FormatterTask
    {
    }

    @Override
    public ConfigDiff transaction(ConfigSource config,
            Schema schema, int taskCount,
            OutputPlugin.Control control)
    {
        final PluginTask task = config.loadConfig(PluginTask.class);
        return resume(task.dump(), schema, taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            OutputPlugin.Control control)
    {
        control.run(taskSource);
        return Exec.newConfigDiff();
    }

    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<TaskReport> successTaskReports)
    { }

    @Override
    public TransactionalPageOutput open(TaskSource taskSource, final Schema schema,
            int taskIndex)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);

        return new TransactionalPageOutput() {
            private final PageReader reader = new PageReader(schema);
            private final PagePrinter printer = new PagePrinter(schema, task);

            public void add(Page page)
            {
                reader.setPage(page);
                while (reader.nextRecord()) {
                    System.out.println(printer.printRecord(reader, ","));
                }
            }

            public void finish()
            {
                System.out.flush();
            }

            public void close()
            {
                reader.close();
            }

            public void abort() { }

            public TaskReport commit()
            {
                return Exec.newTaskReport();
            }
        };
    }
}
