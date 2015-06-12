package org.embulk.exec;

import java.util.Properties;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.embulk.config.ConfigSource;

public class LoggerProvider
        implements Provider<ILoggerFactory>
{
    @Inject
    public LoggerProvider(@ForSystemConfig ConfigSource systemConfig)
    {
        final String level;
        String logLevel = systemConfig.get(String.class, "log_level", "info");  // here can't use loadConfig because ModelManager uses LoggerProvider
        switch (logLevel) {
        case "error": level = "ERROR"; break;
        case "warn":  level = "WARN";  break;
        case "info":  level = "INFO";  break;
        case "debug": level = "DEBUG"; break;
        case "trace": level = "TRACE"; break;
        default:
            throw new IllegalArgumentException(String.format(
                        "System property embulk.logLevel=%s is invalid. Available levels are error, warn, info, debug and trace.", logLevel));
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        context.reset();

        String name;
        if (System.console() != null) {
            name = "/logback-color.xml";
        } else {
            name = "/logback-console.xml";
        }
        try {
            configurator.doConfigure(getClass().getResource(name));
        } catch (JoranException ex) {
            throw new RuntimeException(ex);
        }

        org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger instanceof Logger) {
            ((Logger) logger).setLevel(Level.toLevel(level.toUpperCase(), Level.DEBUG));
        }
    }

    public ILoggerFactory get()
    {
        return LoggerFactory.getILoggerFactory();
    }
}
