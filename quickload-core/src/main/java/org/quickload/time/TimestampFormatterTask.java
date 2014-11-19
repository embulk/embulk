package org.quickload.time;

import org.joda.time.DateTimeZone;
import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.Task;

public interface TimestampFormatterTask
        extends Task
{
    @Config("timezone")
    @ConfigDefault("\"UTC\"")
    public DateTimeZone getTimeZone();
}
