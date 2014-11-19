package org.quickload.time;

import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;

public interface TimestampFormatterTask
{
    @Config("timezone")
    @ConfigDefault("\"UTC\"")
    // TODO TimeZone SerDe
    public String getTimeZone();
}
