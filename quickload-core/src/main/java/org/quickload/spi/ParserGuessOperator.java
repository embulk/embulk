package org.quickload.spi;

import org.quickload.config.ConfigSource;

public interface ParserGuessOperator
        extends BufferOperator
{
    public ConfigSource getGuessedConfig();
}
