package org.quickload.time;

import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.Task;

public interface TimestampParserTask
        extends Task
{
    @Config("default_timezone")
    @ConfigDefault("\"UTC\"")
    // TODO TimeZone SerDe
    public String getDefaultTimeZone();
}
