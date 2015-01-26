package org.embulk.standards;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;
import org.embulk.config.Task;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaVisitor;
import org.embulk.spi.Column;
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
    public NextConfig transaction(ConfigSource config,
            Schema schema, int processorCount,
            OutputPlugin.Control control)
    {
        final PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
        return Exec.newNextConfig();
    }

    @Override
    public TransactionalPageOutput open(TaskSource taskSource, final Schema schema,
            int processorIndex)
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
                page.release();
            }

            public void finish()
            {
                System.out.flush();
            }

            public void close() { }

            public void abort() { }

            public CommitReport commit()
            {
                return Exec.newCommitReport();
            }
        };
    }
}
