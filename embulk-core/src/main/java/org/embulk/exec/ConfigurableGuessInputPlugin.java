package org.embulk.exec;

import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;

public interface ConfigurableGuessInputPlugin
{
    ConfigDiff guess(ConfigSource execConfig, ConfigSource config);
}
