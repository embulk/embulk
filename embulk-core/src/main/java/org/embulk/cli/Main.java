package org.embulk.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import org.embulk.EmbulkVersion;
import org.embulk.deps.EmbulkDependencyClassLoaders;
import org.embulk.deps.EmbulkSelfContainedJarFiles;

public class Main {
    public static void main(final String[] args) {
        // They are loaded before SLF4J is initialized along with Logback. They don't use SLF4J for error logging.
        EmbulkSelfContainedJarFiles.staticInitializer().addFromManifest(CliManifest.getManifest()).initialize();
        EmbulkDependencyClassLoaders.staticInitializer().useSelfContainedJarFiles().initialize();

        final ArrayList<String> jrubyOptions = new ArrayList<String>();

        int i;
        for (i = 0; i < args.length; ++i) {
            if (args[i].startsWith("-R")) {
                jrubyOptions.add(args[i].substring(2));
            } else {
                break;
            }
        }

        final ArrayList<String> embulkArgs = new ArrayList<String>();

        // Process only logging-related options early so that Logback can be configured at the earliest.
        Expect expect = Expect.NONE;
        String logPath = null;
        String logLevel = null;
        for (; i < args.length; ++i) {
            switch (expect) {
                case NONE:
                    if (args[i].startsWith("-l")) {
                        if (args[i].equals("-l")) {
                            expect = Expect.LOG_LEVEL;
                        } else {
                            logLevel = args[i].substring(2);
                            expect = Expect.NONE;
                        }
                    } else if (args[i].equals("--log-level")) {
                        expect = Expect.LOG_LEVEL;
                    } else if (args[i].equals("--log")) {
                        expect = Expect.LOG_PATH;
                    }
                    break;
                case LOG_LEVEL:
                    logLevel = args[i];
                    expect = Expect.NONE;
                    break;
                case LOG_PATH:
                    logPath = args[i];
                    expect = Expect.NONE;
                    break;
                default:
                    break;
            }
            embulkArgs.add(args[i]);
        }

        CliLogbackConfigurator.configure(Optional.ofNullable(logPath), Optional.ofNullable(logLevel));

        final EmbulkRun run = new EmbulkRun(EmbulkVersion.VERSION);
        final int error = run.run(Collections.unmodifiableList(embulkArgs), Collections.unmodifiableList(jrubyOptions));
        System.exit(error);
    }

    private enum Expect {
        NONE,
        LOG_LEVEL,
        LOG_PATH,
        ;
    }
}
