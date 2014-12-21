package org.embulk.spi;

import org.embulk.config.NextConfig;
import org.embulk.config.ConfigSource;

public interface GuessPlugin
{
    public NextConfig guess(ConfigSource config, Buffer sample);
}
