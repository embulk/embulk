package org.embulk.deps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a pre-defined set of class loaders to load dependency libraries for the Embulk core.
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
            this.dependencies = new ArrayList<>();
        }

        public StaticInitializer useSelfContainedJarFiles() {
            this.useSelfContainedJarFiles = true;
            return this;
        }

        public StaticInitializer addDependency(final Path path) {
            this.dependencies.add(path);
            return this;
        }

        public StaticInitializer addDependencies(final Collection<Path> paths) {
            this.dependencies.addAll(paths);
            return this;
        }

        public void initialize() {
            initializeUseSelfContainedJarFiles(this.useSelfContainedJarFiles);
            initializeDependencies(this.dependencies);
        }

        private boolean useSelfContainedJarFiles;
        private final ArrayList<Path> dependencies;
    }

    public static StaticInitializer staticInitializer() {
        return new StaticInitializer();
    }

    public static ClassLoader get() {
        return Holder.DEPENDENCY_CLASS_LOADER;
    }

    private static class Holder {  // Initialization-on-demand holder
        public static final DependencyClassLoader DEPENDENCY_CLASS_LOADER;

        static {
            if (DEPENDENCIES.isEmpty() && !USE_SELF_CONTAINED_JAR_FILES.get()) {
                logger.warn("Hidden dependencies are uninitialized. Maybe using classes loaded by Embulk's top-level ClassLoader.");
            }
            DEPENDENCY_CLASS_LOADER = new DependencyClassLoader(DEPENDENCIES, CLASS_LOADER);
        }
    }

    private static void initializeUseSelfContainedJarFiles(final boolean useSelfContainedJarFile) {
        USE_SELF_CONTAINED_JAR_FILES.set(useSelfContainedJarFile);
    }

    private static void initializeDependencies(final ArrayList<Path> dependencies) {
        synchronized (DEPENDENCIES) {
            if (DEPENDENCIES.isEmpty()) {
                DEPENDENCIES.addAll(dependencies);
            } else {
                throw new LinkageError("Double initialization of hidden dependencies.");
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(EmbulkDependencyClassLoaders.class);

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.class.getClassLoader();
    private static final ArrayList<Path> DEPENDENCIES = new ArrayList<>();

    private static final AtomicBoolean USE_SELF_CONTAINED_JAR_FILES = new AtomicBoolean(false);
}
