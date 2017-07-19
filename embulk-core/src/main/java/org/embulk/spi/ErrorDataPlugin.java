package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;

public interface ErrorDataPlugin
{
    TaskSource createTaskSource(final ConfigSource config);

    ErrorDataReporter open(final TaskSource taskSource);
}
