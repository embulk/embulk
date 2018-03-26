package org.embulk.spi;

import org.embulk.config.ConfigSource;

public interface ErrorDataPlugin {
    ErrorDataReporter open(ConfigSource configSource);
}
