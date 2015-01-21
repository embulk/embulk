package org.embulk.exec;

import java.util.ServiceLoader;
import com.google.inject.Module;
import com.google.inject.Binder;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Extension;

/**
 * ExtensionServiceLoaderModule loads Extensions using java.util.ServiceLoader
 * mechanism.
 * Jar packages providing an extension need to include
 * META-INF/services/org.embulk.exec.Extension file. Contents of the file is
 * one-line text of the extension class name (e.g. com.example.MyPluginSourceExtension).
 */
public class ExtensionServiceLoaderModule
        implements Module
{
    private final ClassLoader classLoader;
    private final ConfigSource systemConfig;

    public ExtensionServiceLoaderModule(ConfigSource systemConfig)
    {
        this(ExtensionServiceLoaderModule.class.getClassLoader(), systemConfig);
    }

    public ExtensionServiceLoaderModule(ClassLoader classLoader, ConfigSource systemConfig)
    {
        this.classLoader = classLoader;
        this.systemConfig = systemConfig;
    }

    @Override
    public void configure(Binder binder)
    {
        ServiceLoader<Extension> serviceLoader = ServiceLoader.load(Extension.class, classLoader);
        for (Extension extension : serviceLoader) {
            for (Module module : extension.getModules(systemConfig)) {
                module.configure(binder);
            }
        }
    }
}
