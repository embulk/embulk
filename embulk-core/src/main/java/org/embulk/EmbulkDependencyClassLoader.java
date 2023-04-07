package org.embulk;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.embulk.cli.SelfContainedJarAwareURLClassLoader;
import org.embulk.cli.SelfContainedJarFiles;

/**
 * A singleton class loader to load classes of embulk-core's hidden dependency libraries, such as Jackson.
 *
 * <p>This class loader is introduced only to control the visibility from plugins, not for customizability.
 * It is intentionally designed to be a singleton.
 */
public final class EmbulkDependencyClassLoader extends SelfContainedJarAwareURLClassLoader {
    private EmbulkDependencyClassLoader(
            final Collection<Path> jarPaths, final ClassLoader parent, final boolean useSelfContainedJarFile) {
        // The delegation parent ClassLoader is processed by the super class URLClassLoader.
        super(toUrls(jarPaths), parent, useSelfContainedJarFile ? SelfContainedJarFiles.CORE : null);
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

    @Override
    protected void addURL(final URL url) {
        throw new UnsupportedOperationException("EmbulkDependencyClassLoader does not support addURL.");
    }

    @Override
    public void close() throws IOException {
        super.close();
        // TODO: Close SelfContainedJarFiles?
    }

    @Override
    public URL[] getURLs() {
        return super.getURLs();  // TODO: Add jar: URLs of self-contained JAR files.
    }

    private static class Holder {  // Initialization-on-demand holder
        public static final EmbulkDependencyClassLoader DEPENDENCY_CLASS_LOADER;

        static {
            if (DEPENDENCIES.isEmpty() && !USE_SELF_CONTAINED_JAR_FILES.get()) {
                System.err.println(
                        "Hidden dependencies are uninitialized. Maybe using classes loaded by Embulk's top-level ClassLoader.");
            }
            DEPENDENCY_CLASS_LOADER =
                    new EmbulkDependencyClassLoader(DEPENDENCIES, CLASS_LOADER, USE_SELF_CONTAINED_JAR_FILES.get());
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

    private static URL[] toUrls(final Collection<Path> jarPaths) {
        final URL[] jarUrls = new URL[jarPaths.size()];

        int index = 0;
        for (final Path jarPath : jarPaths) {
            try {
                jarUrls[index] = jarPath.toUri().toURL();
            } catch (final MalformedURLException ex) {
                throw new LinkageError("Invalid path to JAR: " + jarPath.toString(), ex);
            }
            ++index;
        }
        return jarUrls;
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoader.class.getClassLoader();
    private static final ArrayList<Path> DEPENDENCIES = new ArrayList<>();

    private static final AtomicBoolean USE_SELF_CONTAINED_JAR_FILES = new AtomicBoolean(false);
}
