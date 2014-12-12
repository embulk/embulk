package org.embulk.time;

import org.joda.time.DateTimeZone;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.Task;

public interface TimestampParserTask
        extends Task
{
    @Config("default_timezone")
    @ConfigDefault("\"UTC\"")
    public DateTimeZone getDefaultTimeZone();
}
