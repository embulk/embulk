package org.embulk.exec;

import com.google.inject.Provider;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public class LoggerProvider implements Provider<ILoggerFactory> {
    public LoggerProvider() {
        // Logback is initialized by org.embulk.cli.CliLogbackConfigurator only when executed from CLI.
    }

    public ILoggerFactory get() {
        return LoggerFactory.getILoggerFactory();
    }
}
