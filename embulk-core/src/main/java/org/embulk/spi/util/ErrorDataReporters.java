package org.embulk.spi.util;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.ErrorDataPlugin;

public class ErrorDataReporters
{
    public static void transaction(ErrorDataPlugin plugin, ConfigSource config, Control control)
    {
        final TaskSource taskSource = plugin.createTaskSource(config);
        control.run(taskSource);
    }

    public interface Control
    {
        public void run(TaskSource taskSource);
    }
}
