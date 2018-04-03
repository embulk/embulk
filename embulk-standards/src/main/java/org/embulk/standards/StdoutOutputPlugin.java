package org.embulk.standards;

import java.util.List;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.Exec;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.TransactionalPageOutput;
import org.embulk.spi.time.TimeZoneIds;
import org.embulk.spi.util.PagePrinter;

public class StdoutOutputPlugin implements OutputPlugin {
    public interface PluginTask extends Task {
        @Config("prints_column_names")
        @ConfigDefault("false")
        public boolean getPrintsColumnNames();

        @Config("timezone")
        @ConfigDefault("\"UTC\"")
        public String getTimeZoneId();

        // Using Joda-Time is deprecated, but the getter returns org.joda.time.DateTimeZone for plugin compatibility.
        // It won't be removed very soon at least until Embulk v0.10.
        @Deprecated
        public default org.joda.time.DateTimeZone getTimeZone() {
            if (getTimeZoneId() != null) {
                return TimeZoneIds.parseJodaDateTimeZone(getTimeZoneId());
            } else {
                return null;
            }
        }
    }

    @Override
    public ConfigDiff transaction(ConfigSource config,
            Schema schema, int taskCount,
            OutputPlugin.Control control) {
        final PluginTask task = config.loadConfig(PluginTask.class);
        if (task.getPrintsColumnNames()) {
            for (final Column column : schema.getColumns()) {
                if (column.getIndex() > 0) {
                    System.out.print(",");
                }
                System.out.print(column.getName());
            }
            System.out.println("");
        }
        return resume(task.dump(), schema, taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            OutputPlugin.Control control) {
        control.run(taskSource);
        return Exec.newConfigDiff();
    }

    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<TaskReport> successTaskReports) {}

    @Override
    public TransactionalPageOutput open(TaskSource taskSource, final Schema schema,
            int taskIndex) {
        final PluginTask task = taskSource.loadTask(PluginTask.class);

        return new TransactionalPageOutput() {
            private final PageReader reader = new PageReader(schema);
            private final PagePrinter printer = new PagePrinter(schema, task.getTimeZoneId());

            public void add(Page page) {
                reader.setPage(page);
                while (reader.nextRecord()) {
                    System.out.println(printer.printRecord(reader, ","));
                }
            }

            public void finish() {
                System.out.flush();
            }

            public void close() {
                reader.close();
            }

            public void abort() {}

            public TaskReport commit() {
                return Exec.newTaskReport();
            }
        };
    }
}
