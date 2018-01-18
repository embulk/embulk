package org.embulk.exec;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;

public interface ConfigurableGuessInputPlugin {
    ConfigDiff guess(ConfigSource execConfig, ConfigSource config);
}
