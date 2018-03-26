package org.embulk.spi.util;

import org.embulk.spi.ErrorDataReporter;

public class DefaultErrorDataReporter implements ErrorDataReporter {
    public static ErrorDataReporter create() {
        return new DefaultErrorDataReporter();
    }

    private DefaultErrorDataReporter() {
    }

    @Override
    public void skip(String reason) {
    }

    @Override
    public void close() {
    }
}
