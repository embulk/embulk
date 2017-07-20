package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;

public interface ReporterPlugin
{
    TaskSource configureTaskSource(final ConfigSource config);

    Reporter open(final TaskSource errorDataTask);
}
