package org.quickload.time;

import org.joda.time.DateTimeZone;
import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.Task;

public interface TimestampParserTask
        extends Task
{
    @Config("default_timezone")
    @ConfigDefault("\"UTC\"")
    public DateTimeZone getDefaultTimeZone();
}
