package org.embulk.exec;

import java.util.Properties;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.apache.log4j.PropertyConfigurator;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.embulk.config.ConfigSource;

public class LoggerProvider
        implements Provider<ILoggerFactory>
{
    @Inject
    public LoggerProvider(@ForSystemConfig ConfigSource systemConfig)
    {
        // TODO system config
        Properties prop = new Properties();

        final String level;
        String logLevel = systemConfig.get(String.class, "logLevel", "info");
        switch (logLevel) {
        case "fatal": level = "FATAL"; break;
        case "error": level = "ERROR"; break;
        case "warn":  level = "WARN";  break;
        case "info":  level = "INFO";  break;
        case "debug": level = "DEBUG"; break;
        case "trace": level = "TRACE"; break;
        default:
            throw new IllegalArgumentException(String.format(
                        "System property embulk.logLevel=%s is invalid. Available levels are fatal, error, warn, info, debug and trace.", logLevel));
        }

        prop.setProperty("log4j.rootLogger", level+",root");
        prop.setProperty("log4j.appender.root", "org.apache.log4j.ConsoleAppender");
        prop.setProperty("log4j.appender.root.layout", "org.apache.log4j.PatternLayout");
        prop.setProperty("log4j.appender.root.layout.ConversionPattern", "%d [%p]: %t:%c: %m%n");

        // TODO
        PropertyConfigurator.configure(prop);
    }

    public ILoggerFactory get()
    {
        return LoggerFactory.getILoggerFactory();
    }
}
