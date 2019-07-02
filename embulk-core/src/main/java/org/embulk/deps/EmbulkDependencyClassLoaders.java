package org.embulk.deps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a set of class loaders to load dependency libraries for the Embulk core.
 *
 * <p>It is intentionally designed to be a singleton. {@link DependencyClassLoader} is introduced only for dependency
 * visibility, not for customizability. It should not be customizable so flexibly.
 */
public final class EmbulkDependencyClassLoaders {
    private EmbulkDependencyClassLoaders() {
        // No instantiation.
    }

    public static final class StaticInitializer {
        private StaticInitializer() {
            this.useSelfContainedJarFiles = false;
            this.mavenDependencies = new ArrayList<>();
            this.cliDependencies = new ArrayList<>();
            this.timestampDependencies = new ArrayList<>();
        }

        public StaticInitializer useSelfContainedJarFiles() {
            this.useSelfContainedJarFiles = true;
            return this;
        }

        public StaticInitializer addMavenDependency(final Path path) {
            this.mavenDependencies.add(path);
            return this;
        }

        public StaticInitializer addMavenDependencies(final Collection<Path> paths) {
            this.mavenDependencies.addAll(paths);
            return this;
        }

        public StaticInitializer addCliDependency(final Path path) {
            this.cliDependencies.add(path);
            return this;
        }

        public StaticInitializer addCliDependencies(final Collection<Path> paths) {
            this.cliDependencies.addAll(paths);
            return this;
        }

        public StaticInitializer addTimestampDependency(final Path path) {
            this.timestampDependencies.add(path);
            return this;
        }

        public StaticInitializer addTimestampDependencies(final Collection<Path> paths) {
            this.timestampDependencies.addAll(paths);
            return this;
        }

        public void initialize() {
            initializeUseSelfContainedJarFiles(this.useSelfContainedJarFiles);
            initializeMaven(Collections.unmodifiableList(this.mavenDependencies));
            initializeCli(Collections.unmodifiableList(this.cliDependencies));
            initializeTimestamp(Collections.unmodifiableList(this.timestampDependencies));
        }

        private boolean useSelfContainedJarFiles;
        private final ArrayList<Path> mavenDependencies;
        private final ArrayList<Path> cliDependencies;
        private final ArrayList<Path> timestampDependencies;
    }

    public static StaticInitializer staticInitializer() {
        return new StaticInitializer();
    }

    public static ClassLoader ofMaven() {
        return MavenHolder.MAVEN_DEPENDENCY_CLASS_LOADER;
    }

    public static ClassLoader ofCli() {
        return CliHolder.CLI_DEPENDENCY_CLASS_LOADER;
    }

    public static ClassLoader ofTimestamp() {
        return TimestampHolder.TIMESTAMP_DEPENDENCY_CLASS_LOADER;
    }

    private static class MavenHolder {  // Initialization-on-demand holder
        public static final DependencyClassLoader MAVEN_DEPENDENCY_CLASS_LOADER;

        static {
            if (MAVEN_DEPENDENCIES.isEmpty() && !USE_SELF_CONTAINED_JAR_FILES.get()) {
                logger.warn(messageUninitialized("Maven"));
            }
            MAVEN_DEPENDENCY_CLASS_LOADER = new DependencyClassLoader(
                    MAVEN_DEPENDENCIES,
                    CLASS_LOADER,
                    USE_SELF_CONTAINED_JAR_FILES.get()
                            ? EmbulkSelfContainedJarFiles.Type.MAVEN
                            : EmbulkSelfContainedJarFiles.Type.NONE);
        }
    }

    private static class CliHolder {  // Initialization-on-demand holder
        public static final DependencyClassLoader CLI_DEPENDENCY_CLASS_LOADER;

        static {
            if (CLI_DEPENDENCIES.isEmpty() && !USE_SELF_CONTAINED_JAR_FILES.get()) {
                logger.warn(messageUninitialized("CLI"));
            }
            CLI_DEPENDENCY_CLASS_LOADER = new DependencyClassLoader(
                    CLI_DEPENDENCIES,
                    CLASS_LOADER,
                    USE_SELF_CONTAINED_JAR_FILES.get()
                            ? EmbulkSelfContainedJarFiles.Type.CLI
                            : EmbulkSelfContainedJarFiles.Type.NONE);
        }
    }

    private static class TimestampHolder {  // Initialization-on-demand holder
        public static final DependencyClassLoader TIMESTAMP_DEPENDENCY_CLASS_LOADER;

        static {
            if (TIMESTAMP_DEPENDENCIES.isEmpty() && !USE_SELF_CONTAINED_JAR_FILES.get()) {
                logger.warn(messageUninitialized("timestamp"));
            }
            TIMESTAMP_DEPENDENCY_CLASS_LOADER = new DependencyClassLoader(
                    TIMESTAMP_DEPENDENCIES,
                    CLASS_LOADER,
                    USE_SELF_CONTAINED_JAR_FILES.get()
                            ? EmbulkSelfContainedJarFiles.Type.TIMESTAMP
                            : EmbulkSelfContainedJarFiles.Type.NONE);
        }
    }

    private static void initializeUseSelfContainedJarFiles(final boolean useSelfContainedJarFile) {
        USE_SELF_CONTAINED_JAR_FILES.set(useSelfContainedJarFile);
    }

    private static void initializeMaven(final List<Path> mavenDependencies) {
        synchronized (MAVEN_DEPENDENCIES) {
            if (MAVEN_DEPENDENCIES.isEmpty()) {
                MAVEN_DEPENDENCIES.addAll(mavenDependencies);
            } else {
                throw new LinkageError("Double initialization of dependencies for Maven.");
            }
        }
    }

    private static void initializeCli(final List<Path> cliDependencies) {
        synchronized (CLI_DEPENDENCIES) {
            if (CLI_DEPENDENCIES.isEmpty()) {
                CLI_DEPENDENCIES.addAll(cliDependencies);
            } else {
                throw new LinkageError("Double initialization of dependencies for CLI.");
            }
        }
    }

    private static void initializeTimestamp(final List<Path> timestampDependencies) {
        synchronized (TIMESTAMP_DEPENDENCIES) {
            if (TIMESTAMP_DEPENDENCIES.isEmpty()) {
                TIMESTAMP_DEPENDENCIES.addAll(timestampDependencies);
            } else {
                throw new LinkageError("Double initialization of dependencies for timestamp.");
            }
        }
    }

    private static String messageUninitialized(final String target) {
        return String.format(
                "Dependencies for %s are uninitialized. Expected to use classes loaded by the parent ClassLoader.", target);
    }

    private static final Logger logger = LoggerFactory.getLogger(EmbulkDependencyClassLoaders.class);

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.class.getClassLoader();

    private static final ArrayList<Path> MAVEN_DEPENDENCIES = new ArrayList<>();
    private static final ArrayList<Path> CLI_DEPENDENCIES = new ArrayList<>();
    private static final ArrayList<Path> TIMESTAMP_DEPENDENCIES = new ArrayList<>();

    private static final AtomicBoolean USE_SELF_CONTAINED_JAR_FILES = new AtomicBoolean(false);
}
