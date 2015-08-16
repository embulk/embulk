package org.embulk.plugin;

import java.util.Collection;
import java.util.Properties;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import com.google.inject.Module;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.Provider;
import org.embulk.config.ConfigSource;

public class PluginClassLoaderModule
        implements Module
{
    public PluginClassLoaderModule(ConfigSource systemConfig)
    { }

    @Override
    public void configure(Binder binder)
    {
        binder.bind(PluginClassLoaderFactory.class).toProvider(new FactoryProvider()).in(Scopes.SINGLETON);;
    }

    private static class FactoryProvider
            implements Provider<PluginClassLoaderFactory>
    {
        private final Collection<String> parentFirstPackages;
        private final Collection<String> parentFirstResources;
        private final PluginClassLoaderFactory factory;

        public FactoryProvider()
        {
            // TODO make these paths customizable using ConfigSource
            this.parentFirstPackages = readPropertyKeys("/embulk/parent_first_packages.properties");
            this.parentFirstResources = readPropertyKeys("/embulk/parent_first_resources.properties");

            this.factory = new Factory();
        }

        private static Collection<String> readPropertyKeys(String name)
        {
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
        public PluginClassLoaderFactory get()
        {
            return factory;
        }

        private class Factory implements PluginClassLoaderFactory
        {
            public PluginClassLoader create(Collection<URL> urls, ClassLoader parentClassLoader)
            {
                return new PluginClassLoader(urls, parentClassLoader,
                        parentFirstPackages, parentFirstResources);
            }
        }
    }
}
