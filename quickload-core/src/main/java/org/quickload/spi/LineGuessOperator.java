package org.quickload.spi;

import org.quickload.config.ConfigSource;

public interface LineGuessOperator
        extends BufferOperator
{
    public ConfigSource getGuessedConfig();
}
