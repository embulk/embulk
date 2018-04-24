package org.embulk.plugin;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;
import org.embulk.config.ConfigSource;

public class PluginClassLoaderModule implements Module {
    public PluginClassLoaderModule(ConfigSource systemConfig) {}

    @Override
    public void configure(Binder binder) {
        binder.bind(PluginClassLoaderFactory.class).toProvider(new FactoryProvider()).in(Scopes.SINGLETON);
    }

    private static class FactoryProvider implements Provider<PluginClassLoaderFactory> {
        private final Collection<String> parentFirstPackages;
        private final Collection<String> parentFirstResources;
        private final PluginClassLoaderFactory factory;

        public FactoryProvider() {
            // TODO make these paths customizable using ConfigSource
            this.parentFirstPackages = readPropertyKeys("/embulk/parent_first_packages.properties");
            this.parentFirstResources = readPropertyKeys("/embulk/parent_first_resources.properties");

            this.factory = new Factory();
        }

        private static Collection<String> readPropertyKeys(String name) {
            try (InputStream in = PluginClassLoaderModule.class.getResourceAsStream(name)) {
                if (in == null) {
                    throw new NullPointerException(String.format("Resource '%s' is not found in classpath. Jar file or classloader is broken.", name));
                }
                Properties prop = new Properties();
                prop.load(in);
                return prop.stringPropertyNames();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public PluginClassLoaderFactory get() {
            return factory;
        }

        private class Factory implements PluginClassLoaderFactory {
            @Override
            public PluginClassLoader create(Collection<URL> urls, ClassLoader parentClassLoader) {
                return PluginClassLoader.createForFlatJars(
                        parentClassLoader,
                        urls,
                        parentFirstPackages,
                        parentFirstResources);
            }

            @Override
            public PluginClassLoader createForNestedJar(
                    final ClassLoader parentClassLoader,
                    final URL oneNestedJarUrl) {
                return PluginClassLoader.createForNestedJar(
                        parentClassLoader,
                        oneNestedJarUrl,
                        null,
                        parentFirstPackages,
                        parentFirstResources);
            }

            @Override
            public PluginClassLoader createForNestedJar(
                    final ClassLoader parentClassLoader,
                    final URL oneNestedJarUrl,
                    final Collection<String> embeddedJarPathsInNestedJar) {
                return PluginClassLoader.createForNestedJar(
                        parentClassLoader,
                        oneNestedJarUrl,
                        embeddedJarPathsInNestedJar,
                        parentFirstPackages,
                        parentFirstResources);
            }

            @Override
            public PluginClassLoader createForNestedJarWithDependencies(
                    final ClassLoader parentClassLoader,
                    final URL oneNestedJarUrl,
                    final Collection<String> embeddedJarPathsInNestedJar,
                    final Collection<URL> dependencyJarUrls) {
                return PluginClassLoader.createForNestedJar(
                        parentClassLoader,
                        oneNestedJarUrl,
                        embeddedJarPathsInNestedJar,
                        dependencyJarUrls,
                        parentFirstPackages,
                        parentFirstResources);
            }
        }
    }
}
