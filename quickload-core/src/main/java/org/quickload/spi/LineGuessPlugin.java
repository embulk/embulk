package org.quickload.spi;

import org.quickload.config.ConfigSource;

public interface LineGuessPlugin
{
    public LineGuessOperator openLineGuessOperator(ConfigSource config);

    public void shutdown();
}
