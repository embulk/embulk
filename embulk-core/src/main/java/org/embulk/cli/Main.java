package org.embulk.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.jar.Manifest;
import org.embulk.EmbulkDependencyClassLoader;
import org.embulk.EmbulkSystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

public class Main {
    public static void main(final String[] args) {
        final Manifest manifest = CliManifest.getManifest();

        // They are loaded before SLF4J is initialized along with Logback. They don't use SLF4J for error logging.
        SelfContainedJarFiles.staticInitializer().addFromManifest(manifest).initialize();
        EmbulkDependencyClassLoader.staticInitializer().useSelfContainedJarFiles().initialize();

        // Using SubstituteLogger here because SLF4J and Logback are initialized later (CliLogbackConfigurator.configure).
        final LinkedBlockingQueue<SubstituteLoggingEvent> loggingEventQueue = new LinkedBlockingQueue<SubstituteLoggingEvent>();
        final SubstituteLogger substituteLogger = new SubstituteLogger(Main.class.getName(), loggingEventQueue, false);

        final CommandLineParser commandLineParser = CommandLineParser.create();
        final CommandLine commandLine = commandLineParser.parse(Arrays.asList(args), substituteLogger);
        System.err.print(commandLine.getStdErr());
        System.out.print(commandLine.getStdOut());

        final Properties commandLineProperties = commandLine.getCommandLineProperties();
        final EmbulkSystemPropertiesBuilder embulkSystemPropertiesBuilder = EmbulkSystemPropertiesBuilder.from(
                System.getProperties(), commandLineProperties, System.getenv(), manifest, substituteLogger);
        final EmbulkSystemProperties embulkSystemProperties = embulkSystemPropertiesBuilder.buildProperties();

        CliLogbackConfigurator.configure(embulkSystemProperties);

        // Creating a real logger after SLF4J is initialized along with Logback. Flushing recorded logs by SubstituteLogger, then.
        final Logger mainLogger = LoggerFactory.getLogger(Main.class);
        flushLogs(substituteLogger, loggingEventQueue, mainLogger);

        if (!commandLine.isValid()) {
            System.exit(-1);
            return;
        }

        if (mainLogger.isDebugEnabled()) {
            mainLogger.debug("Command-line arguments: {}", commandLine.getArguments());
            mainLogger.debug("Embulk system properties: {}", embulkSystemProperties);
        }

        System.exit(EmbulkRun.run(commandLine, embulkSystemProperties));
    }

    private static void flushLogs(
            final SubstituteLogger substituteLogger,
            final LinkedBlockingQueue<SubstituteLoggingEvent> loggingEventQueue,
            final Logger mainLogger) {
        substituteLogger.setDelegate(mainLogger);
        final ArrayList<SubstituteLoggingEvent> events = new ArrayList<>();
        loggingEventQueue.drainTo(events);
        for (final SubstituteLoggingEvent event : events) {
            substituteLogger.log(event);
        }
    }
}
