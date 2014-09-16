package org.quickload.spi;

import org.quickload.config.ConfigSource;

public interface GuessableInputPlugin
        extends InputPlugin
{
    public ConfigSource guess(ConfigSource config);
}
