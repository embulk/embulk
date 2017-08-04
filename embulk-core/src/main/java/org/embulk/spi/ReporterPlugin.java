package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;

public interface ReporterPlugin
{
    TaskSource configureTaskSource(final ConfigSource config);

    AbstractReporterImpl open(final TaskSource task);
}
