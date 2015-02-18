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

        String logLevel = systemConfig.get(String.class, "logLevel","INFO");
        prop.setProperty("log4j.rootLogger", logLevel+",root");
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
