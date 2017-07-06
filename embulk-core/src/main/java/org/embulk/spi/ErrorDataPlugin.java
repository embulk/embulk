package org.embulk.spi;

import org.embulk.config.TaskSource;

public interface ErrorDataPlugin
{
    ErrorDataReporter open(TaskSource taskSource);
}
