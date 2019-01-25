package org.embulk.cli;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Optional;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures Logback's root logger.
 *
 * <p>It calls Logback's configurators through reflection so that embulk-core does not need to depend on Logback's classes.
 */
@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:MemberName", "checkstyle:ParameterName"})
final class CliLogbackConfigurator {
    static void configure(final Optional<String> logPath, final Optional<String> logLevel) {
        final URL configurationResource = chooseConfigurationResource(logPath);
        final ClassLoader classLoader = CliLogbackConfigurator.class.getClassLoader();
        try {
            new CliLogbackConfigurator(classLoader).configureLogger(logLevel, configurationResource);
        } catch (final LogbackException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof ReflectiveOperationException) {
                System.err.println("Failed in loading Logback classes.");
            } else {
                System.err.println("Failed in configuring Logback.");
            }
            cause.printStackTrace();
        }
    }

    private CliLogbackConfigurator(final ClassLoader classLoader) throws LogbackException {
        try {
            this.Level_class = classLoader.loadClass("ch.qos.logback.classic.Level");
            this.Logger_class = classLoader.loadClass("ch.qos.logback.classic.Logger");
            this.LoggerContext_class = classLoader.loadClass("ch.qos.logback.classic.LoggerContext");
            this.JoranConfigurator_class = classLoader.loadClass("ch.qos.logback.classic.joran.JoranConfigurator");
            this.Context_class = classLoader.loadClass("ch.qos.logback.core.Context");
            this.JoranException_class = classLoader.loadClass("ch.qos.logback.core.joran.spi.JoranException");

            this.OFF = getStaticField(this.Level_class, "OFF");
            this.ERROR = getStaticField(this.Level_class, "ERROR");
            this.WARN = getStaticField(this.Level_class, "WARN");
            this.INFO = getStaticField(this.Level_class, "INFO");
            this.DEBUG = getStaticField(this.Level_class, "DEBUG");
            this.TRACE = getStaticField(this.Level_class, "TRACE");

            this.constructJoranConfigurator = this.JoranConfigurator_class.getConstructor();

            this.setLevel = this.Logger_class.getMethod("setLevel", this.Level_class);
            this.reset = this.LoggerContext_class.getMethod("reset");
            this.setContext = this.JoranConfigurator_class.getMethod("setContext", this.Context_class);
            this.doConfigure = this.JoranConfigurator_class.getMethod("doConfigure", URL.class);
        } catch (final ReflectiveOperationException ex) {
            throw new LogbackException(ex);
        }
    }

    private void configureLogger(final Optional<String> logLevel, final URL configurationResource) throws LogbackException {
        /*
         * LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
         */
        final ILoggerFactory context = LoggerFactory.getILoggerFactory();
        if (!context.getClass().isAssignableFrom(this.LoggerContext_class)) {
            throw new ClassCastException(
                    "LoggerFactory.getILoggerFactory() did not return logback's ch.qos.logback.classic.LoggerContext.");
        }

        /*
         * JoranConfigurator configurator = new JoranConfigurator();
         * configurator.setContext(context);
         * context.reset();
         */
        final Object configuratorObject;
        try {
            configuratorObject = this.constructJoranConfigurator.newInstance();
            this.setContext.invoke(configuratorObject, context);
            this.reset.invoke(context);
        } catch (final ReflectiveOperationException ex) {
            throw new LogbackException(ex);
        }

        /*
         * try {
         *     configurator.doConfigure(getClass().getResource(name));
         * } catch (JoranException ex) {
         *     throw new RuntimeException(ex);
         * }
         */
        try {
            this.doConfigure.invoke(configuratorObject, configurationResource);
        } catch (final ReflectiveOperationException ex) {
            if (ex instanceof InvocationTargetException) {
                final Throwable targetException = ((InvocationTargetException) ex).getTargetException();
                if (this.JoranException_class.isAssignableFrom(targetException.getClass())) {
                    throw new RuntimeException(targetException);
                }
            }
            throw new LogbackException(ex);
        }

        final Logger logger = getRootLogger();

        /*
         * if (logger instanceof Logger) {
         *     ((Logger) logger).setLevel(Level.toLevel(level.toUpperCase(), Level.DEBUG));
         * }
         */
        if (this.Logger_class.isAssignableFrom(logger.getClass())) {
            try {
                this.setLevel.invoke(logger, this.chooseLevel(logLevel));
            } catch (final ReflectiveOperationException ex) {
                throw new LogbackException(ex);
            }
        }

    }

    private static URL chooseConfigurationResource(final Optional<String> logPath) {
        if (logPath.orElse("-").equals("-")) {
            if (System.console() != null) {
                return CliLogbackConfigurator.class.getResource("/embulk/logback-color.xml");
            } else {
                return CliLogbackConfigurator.class.getResource("/embulk/logback-console.xml");
            }
        }

        // logback uses system property to embed variables in XML file
        System.setProperty("embulk.logPath", logPath.get());
        return CliLogbackConfigurator.class.getResource("/embulk/logback-file.xml");
    }

    private Object chooseLevel(final Optional<String> levelString) {
        switch (levelString.orElse("info")) {
            case "error":
                return this.ERROR;
            case "warn":
                return this.WARN;
            case "info":
                return this.INFO;
            case "debug":
                return this.DEBUG;
            case "trace":
                return this.TRACE;
            default:
                throw new IllegalArgumentException(String.format(
                        "Log level %s is invalid. Available levels are error, warn, info, debug and trace.",
                        levelString.orElse("(null)")));
        }
    }

    private static Logger getRootLogger() throws LogbackException {
        // org.slf4j.Logger.ROOT_LOGGER_NAME is fine. ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME inherits slf4j's.
        final Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (logger == null) {
            throw new NullPointerException("The root logger is unexpectedly null.");
        }
        if (!logger.getClass().getName().equals("ch.qos.logback.classic.Logger")) {
            throw new ClassCastException("The root logger is unexpectedly not ch.qos.logback.classic.Logger.");
        }
        return logger;
    }

    private static Object getStaticField(final Class<?> clazz, final String fieldName) throws LogbackException {
        try {
            final Field field = clazz.getField(fieldName);
            return field.get(null);
        } catch (final ReflectiveOperationException ex) {
            throw new LogbackException(ex);
        }
    }

    private static final class LogbackException extends ReflectiveOperationException {
        public LogbackException(final ReflectiveOperationException cause) {
            super(pickupException(cause));
        }

        private static Throwable pickupException(final ReflectiveOperationException cause) {
            if (cause instanceof InvocationTargetException) {
                return ((InvocationTargetException) cause).getTargetException();
            } else {
                return cause;
            }
        }
    }

    private final Class<?> Level_class;
    private final Class<?> Logger_class;
    private final Class<?> LoggerContext_class;
    private final Class<?> JoranException_class;
    private final Class<?> Context_class;
    private final Class<?> JoranConfigurator_class;

    private final Object OFF;
    private final Object ERROR;
    private final Object WARN;
    private final Object INFO;
    private final Object DEBUG;
    private final Object TRACE;

    private final Constructor<?> constructJoranConfigurator;

    private final Method setLevel;
    private final Method reset;
    private final Method setContext;
    private final Method doConfigure;
}
