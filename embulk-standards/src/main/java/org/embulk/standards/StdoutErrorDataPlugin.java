package org.embulk.standards;

import org.embulk.config.ConfigSource;
import org.embulk.spi.ErrorDataPlugin;
import org.embulk.spi.ErrorDataReporter;

public class StdoutErrorDataPlugin
        implements ErrorDataPlugin {
    @Override
    public ErrorDataReporter open(ConfigSource configSource) {
        return new StdoutErrorDataReporter();
    }

    private static class StdoutErrorDataReporter
            implements ErrorDataReporter {
        @Override
        public void skip(String errorData) {
            System.out.println(errorData);
        }

        @Override
        public void close() {}
    }
}
