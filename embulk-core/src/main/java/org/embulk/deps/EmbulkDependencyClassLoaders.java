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

        public void initialize() {
            initializeUseSelfContainedJarFiles(this.useSelfContainedJarFiles);
            initializeMaven(Collections.unmodifiableList(this.mavenDependencies));
        }

        private boolean useSelfContainedJarFiles;
        private final ArrayList<Path> mavenDependencies;
    }

    public static StaticInitializer staticInitializer() {
        return new StaticInitializer();
    }

    public static ClassLoader ofMaven() {
        return MavenHolder.MAVEN_DEPENDENCY_CLASS_LOADER;
    }

    private static class MavenHolder {  // Initialization-on-demand holder
        public static final DependencyClassLoader MAVEN_DEPENDENCY_CLASS_LOADER;

        static {
            if (MAVEN_DEPENDENCIES.isEmpty()) {
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

    private static String messageUninitialized(final String target) {
        return String.format(
                "Dependencies for %s are uninitialized. Expected to use classes loaded by the parent ClassLoader.", target);
    }

    private static final Logger logger = LoggerFactory.getLogger(EmbulkDependencyClassLoaders.class);

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.class.getClassLoader();

    private static final ArrayList<Path> MAVEN_DEPENDENCIES = new ArrayList<>();

    private static final AtomicBoolean USE_SELF_CONTAINED_JAR_FILES = new AtomicBoolean(false);
}
