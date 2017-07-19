package org.embulk.spi.util;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.ErrorDataPlugin;

public final class ErrorDataReporters
{
    public static void transaction(final ErrorDataPlugin plugin, final ConfigSource config, final Control control)
    {
        final TaskSource taskSource = plugin.configureTaskSource(config);
        control.run(taskSource);
    }

    public interface Control
    {
        void run(final TaskSource task);
    }
}
