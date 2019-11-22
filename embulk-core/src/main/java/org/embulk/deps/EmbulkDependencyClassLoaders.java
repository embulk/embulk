package org.embulk.deps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
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
            this.dependencies = new EnumMap<>(DependencyCategory.class);
            for (final DependencyCategory category : DependencyCategory.values()) {
                this.dependencies.put(category, new ArrayList<>());
            }
        }

        public StaticInitializer useSelfContainedJarFiles() {
            this.useSelfContainedJarFiles = true;
            return this;
        }

        public StaticInitializer addDependency(final DependencyCategory category, final Path path) {
            this.dependencies.get(category).add(path);
            return this;
        }

        public StaticInitializer addDependencies(final DependencyCategory category, final Collection<Path> paths) {
            this.dependencies.get(category).addAll(paths);
            return this;
        }

        @Deprecated
        public StaticInitializer addMavenDependency(final Path path) {
            return this.addDependency(DependencyCategory.MAVEN, path);
        }

        @Deprecated
        public StaticInitializer addMavenDependencies(final Collection<Path> paths) {
            return this.addDependencies(DependencyCategory.MAVEN, paths);
        }

        @Deprecated
        public StaticInitializer addCliDependency(final Path path) {
            return this.addDependency(DependencyCategory.CLI, path);
        }

        @Deprecated
        public StaticInitializer addCliDependencies(final Collection<Path> paths) {
            return this.addDependencies(DependencyCategory.CLI, paths);
        }

        public void initialize() {
            initializeUseSelfContainedJarFiles(this.useSelfContainedJarFiles);
            initializeDependencies(this.dependencies);
        }

        private boolean useSelfContainedJarFiles;
        private final EnumMap<DependencyCategory, ArrayList<Path>> dependencies;
    }

    public static StaticInitializer staticInitializer() {
        return new StaticInitializer();
    }

    public static ClassLoader of(final DependencyCategory category) {
        if (category == null) {
            throw new LinkageError("No appropriate DependencyClassLoader for null.");
        }
        return Holder.DEPENDENCY_CLASS_LOADERS.get(category);
    }

    @Deprecated
    public static ClassLoader ofMaven() {
        return of(DependencyCategory.MAVEN);
    }

    @Deprecated
    public static ClassLoader ofCli() {
        return of(DependencyCategory.CLI);
    }

    private static class Holder {  // Initialization-on-demand holder
        public static final Map<DependencyCategory, DependencyClassLoader> DEPENDENCY_CLASS_LOADERS;

        static {
            final EnumMap<DependencyCategory, DependencyClassLoader> classLoaders = new EnumMap<>(DependencyCategory.class);
            for (final DependencyCategory category : DependencyCategory.values()) {
                if (DEPENDENCIES.get(category).isEmpty() && !USE_SELF_CONTAINED_JAR_FILES.get()) {
                    logger.warn(
                            "Dependencies for {} are uninitialized. Expected to use classes loaded by the parent ClassLoader.",
                            category.getName());
                }

                classLoaders.put(category, new DependencyClassLoader(
                        DEPENDENCIES.get(category),
                        CLASS_LOADER,
                        USE_SELF_CONTAINED_JAR_FILES.get()
                                ? category.getSelfContainType()
                                : EmbulkSelfContainedJarFiles.Type.NONE));
            }
            DEPENDENCY_CLASS_LOADERS = Collections.unmodifiableMap(classLoaders);
        }
    }

    private static void initializeUseSelfContainedJarFiles(final boolean useSelfContainedJarFile) {
        USE_SELF_CONTAINED_JAR_FILES.set(useSelfContainedJarFile);
    }

    private static void initializeDependencies(final EnumMap<DependencyCategory, ArrayList<Path>> dependencies) {
        synchronized (DEPENDENCIES) {
            for (final DependencyCategory category : DependencyCategory.values()) {
                if (dependencies.containsKey(category)) {
                    if (dependencies.get(category).isEmpty()) {
                        DEPENDENCIES.get(category).addAll(dependencies.get(category));
                    } else {
                        throw new LinkageError("Double initialization of dependencies for " + category.getName() + ".");
                    }
                }
            }
        }
    }

    static {
        final EnumMap<DependencyCategory, ArrayList<Path>> dependencies = new EnumMap<>(DependencyCategory.class);
        for (final DependencyCategory category : DependencyCategory.values()) {
            dependencies.put(category, new ArrayList<>());
        }
        DEPENDENCIES = Collections.unmodifiableMap(dependencies);
    }

    private static final Logger logger = LoggerFactory.getLogger(EmbulkDependencyClassLoaders.class);

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.class.getClassLoader();
    private static final Map<DependencyCategory, ArrayList<Path>> DEPENDENCIES;

    private static final AtomicBoolean USE_SELF_CONTAINED_JAR_FILES = new AtomicBoolean(false);
}
