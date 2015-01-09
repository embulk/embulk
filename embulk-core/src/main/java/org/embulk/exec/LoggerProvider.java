package org.embulk.exec;

import java.util.Properties;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.apache.log4j.PropertyConfigurator;
import com.google.inject.Provider;

public class LoggerProvider
        implements Provider<ILoggerFactory>
{
    public LoggerProvider()
    {
        // TODO system config
        Properties prop = new Properties();

        prop.setProperty("log4j.rootLogger", "INFO,root");
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
