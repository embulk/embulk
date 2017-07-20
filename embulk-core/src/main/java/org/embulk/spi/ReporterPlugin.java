package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;

public interface ReporterPlugin<REPORTER extends Reporter>
{
    TaskSource configureTaskSource(final ConfigSource config);

    REPORTER open(final TaskSource errorDataTask);
}
