package org.embulk.spi;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;

public interface GuessPlugin
{
    ConfigDiff guess(ConfigSource config, Buffer sample);
}
