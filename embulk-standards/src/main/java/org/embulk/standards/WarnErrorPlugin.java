package org.embulk.standards;

import java.util.List;
import org.slf4j.Logger;
import com.google.common.base.Optional;
import org.msgpack.value.Value;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.TaskReport;
import org.embulk.spi.Exec;
import org.embulk.spi.DataException;
import org.embulk.spi.TransactionalValueOutput;
import org.embulk.spi.ErrorPlugin;

public class WarnErrorPlugin
        implements ErrorPlugin
{
    private Logger logger = Exec.getLogger(WarnErrorPlugin.class);

    public static interface PluginTask
            extends Task
    {
        @Config("max_error_records")
        @ConfigDefault("null")
        public Optional<Long> getMaxErrorRecords();
    }

    public void transaction(ConfigSource config, ErrorPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        List<TaskReport> reports = control.run(task.dump());
        long total = 0;
        for (TaskReport report : reports) {
            total += report.get(long.class, "error_count");
        }
        if (task.getMaxErrorRecords().isPresent() && total > task.getMaxErrorRecords().get()) {
            throw new DataException("Number of error records exceeds max_error_records");
        }
        logger.info("Error records: {}", total);
    }

    public TransactionalValueOutput open(TaskSource taskSource)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);
        return new TransactionalValueOutput()
        {
            private long count = 0;
            private long maxErrorRecords = task.getMaxErrorRecords().or(-1L);

            public void add(Value value)
            {
                logger.warn("Error record: "+value);
                count++;
                if (count > maxErrorRecords) {
                    throw new DataException("Number of error records exceeds max_error_records");
                }
            }

            public void abort()
            {
            }

            public TaskReport commit()
            {
                return Exec.newTaskReport()
                    .set("error_count", count);
            }

            public void close()
            {
            }
        };
    }
}
