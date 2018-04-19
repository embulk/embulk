package org.embulk.plugin.maven;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.embulk.config.ConfigSource;
import org.embulk.exec.ForSystemConfig;
import org.embulk.plugin.MavenPluginType;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginSource;
import org.embulk.plugin.PluginSourceNotMatchException;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.jar.InvalidJarPluginException;
import org.embulk.plugin.jar.JarPluginLoader;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileInputRunner;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FileOutputRunner;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;

public class MavenPluginSource implements PluginSource {
    @Inject
    public MavenPluginSource(Injector injector, @ForSystemConfig ConfigSource systemConfig) {
        this.injector = injector;
        this.systemConfig = systemConfig;
    }

    @Override
    public <T> T newPlugin(Class<T> pluginInterface, PluginType pluginType)
            throws PluginSourceNotMatchException {
        final String category;
        if (InputPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "input";
        } else if (OutputPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "output";
        } else if (ParserPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "parser";
        } else if (FormatterPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "formatter";
        } else if (DecoderPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "decoder";
        } else if (EncoderPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "encoder";
        } else if (FilterPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "filter";
        } else if (GuessPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "guess";
        } else if (ExecutorPlugin.class.isAssignableFrom(pluginInterface)) {
            category = "executor";
        } else {
            // unsupported plugin category
            throw new PluginSourceNotMatchException("Plugin interface " + pluginInterface + " is not supported.");
        }

        if (pluginType.getSourceType() != PluginSource.Type.MAVEN) {
            throw new PluginSourceNotMatchException();
        }
        final MavenPluginType mavenPluginType = (MavenPluginType) pluginType;

        final PluginClassLoaderFactory pluginClassLoaderFactory =
                this.injector.getInstance(PluginClassLoaderFactory.class);

        final MavenArtifactFinder mavenArtifactFinder;
        try {
            mavenArtifactFinder = MavenArtifactFinder.create(getLocalMavenRepository());
        } catch (MavenRepositoryNotFoundException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        final MavenPluginPaths pluginPaths;
        try {
            pluginPaths = mavenArtifactFinder.findMavenPluginJarsWithDirectDependencies(
                    mavenPluginType.getGroup(),
                    "embulk-" + category + "-" + mavenPluginType.getName(),
                    mavenPluginType.getClassifier(),
                    mavenPluginType.getVersion());
        } catch (MavenArtifactNotFoundException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        final Class<?> pluginMainClass;
        try (JarPluginLoader loader = JarPluginLoader.load(
                 pluginPaths.getPluginJarPath(),
                 pluginPaths.getPluginDependencyJarPaths(),
                 pluginClassLoaderFactory)) {
            pluginMainClass = loader.getPluginMainClass();
        } catch (InvalidJarPluginException ex) {
            throw new PluginSourceNotMatchException(ex);
        }

        final Object pluginMainObject;
        try {
            // Unlike InjectedPluginSource and JRubyPluginSource,
            // MavenPluginSource does not have "registration" before creating an instance of the plugin class.
            // FileInputPlugin and FileOutputPlugin are wrapped with FileInputRunner and FileOutputRunner here.
            if (FileInputPlugin.class.isAssignableFrom(pluginMainClass)) {
                final FileInputPlugin fileInputPluginMainObject;
                try {
                    fileInputPluginMainObject = (FileInputPlugin) this.injector.getInstance(pluginMainClass);
                } catch (ClassCastException ex) {
                    throw new PluginSourceNotMatchException(
                            "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not file-input.",
                            ex);
                }
                pluginMainObject = new FileInputRunner(fileInputPluginMainObject);
            } else if (FileOutputPlugin.class.isAssignableFrom(pluginMainClass)) {
                final FileOutputPlugin fileOutputPluginMainObject;
                try {
                    fileOutputPluginMainObject = (FileOutputPlugin) this.injector.getInstance(pluginMainClass);
                } catch (ClassCastException ex) {
                    throw new PluginSourceNotMatchException(
                            "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not file-output.",
                            ex);
                }
                pluginMainObject = new FileOutputRunner(fileOutputPluginMainObject);
            } else {
                if (!pluginInterface.isAssignableFrom(pluginMainClass)) {
                    throw new PluginSourceNotMatchException(
                            "Plugin class \"" + pluginMainClass.getName() + "\" is not a valid " + category + " plugin.");
                }
                pluginMainObject = this.injector.getInstance(pluginMainClass);
            }
        } catch (ExceptionInInitializerError ex) {
            throw new PluginSourceNotMatchException(
                    "Plugin class \"" + pluginMainClass.getName()
                            + "\" is not instantiatable due to exception in initialization.",
                    ex);
        } catch (SecurityException ex) {
            throw new PluginSourceNotMatchException(
                    "Plugin class \"" + pluginMainClass.getName()
                            + "\" is not instantiatable due to security manager.",
                    ex);
        }

        try {
            return pluginInterface.cast(pluginMainObject);
        } catch (ClassCastException ex) {
            throw new PluginSourceNotMatchException(
                    "[FATAL/INTERNAL] Plugin class \"" + pluginMainClass.getName() + "\" is not " + category + " actually.",
                    ex);
        }
    }

    private Path getLocalMavenRepository() throws PluginSourceNotMatchException {
        final String m2RepoInSystemConfig = systemConfig.get(String.class, "m2_repo", null);

        if (m2RepoInSystemConfig != null) {
            return Paths.get(m2RepoInSystemConfig);
        }

        String m2RepoInEnv;
        try {
            m2RepoInEnv = System.getenv("M2_REPO");
        } catch (NullPointerException | SecurityException ex) {
            // The Exceptions are just ignored, and the default local Maven repository is used.
            // TODO: Log?
            m2RepoInEnv = null;
        }

        if (m2RepoInEnv != null) {
            return Paths.get(m2RepoInEnv);
        }

        return getEmbulkHome().resolve("lib").resolve("m2").resolve("repository");
    }

    private Path getEmbulkHome() throws PluginSourceNotMatchException {
        final String propertyHome = System.getProperty("user.home");
        if (propertyHome == null) {
            throw new PluginSourceNotMatchException();
        }

        return Paths.get(propertyHome, ".embulk");
    }

    private final Injector injector;
    private final ConfigSource systemConfig;
}
