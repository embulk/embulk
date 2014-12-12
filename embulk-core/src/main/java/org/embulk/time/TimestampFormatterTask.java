package org.embulk.time;

import org.joda.time.DateTimeZone;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.Task;

public interface TimestampFormatterTask
        extends Task
{
    @Config("timezone")
    @ConfigDefault("\"UTC\"")
    public DateTimeZone getTimeZone();
}
