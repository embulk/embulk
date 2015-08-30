package org.embulk.standards;

import java.util.List;
import org.slf4j.Logger;
import org.msgpack.value.Value;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.TaskReport;
import org.embulk.spi.Exec;
import org.embulk.spi.TransactionalValueOutput;
import org.embulk.spi.ErrorPlugin;

public class WarnErrorPlugin
        implements ErrorPlugin
{
    private Logger logger = Exec.getLogger(WarnErrorPlugin.class);

    public static interface PluginTask
            extends Task
    {
    }

    public void transaction(ConfigSource config, ErrorPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        List<TaskReport> reports = control.run(task.dump());
        int total = 0;
        for (TaskReport report : reports) {
            total += report.get(int.class, "error_count");
        }
        logger.info("Error records: {}", total);
    }

    public TransactionalValueOutput open(TaskSource taskSource)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        return new TransactionalValueOutput()
        {
            private int count = 0;

            public void add(Value value)
            {
                logger.warn("Error record: "+value);
                count++;
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
