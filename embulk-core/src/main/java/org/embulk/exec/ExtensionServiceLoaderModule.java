package org.embulk.exec;

import java.util.ServiceLoader;
import com.google.inject.Module;
import com.google.inject.Binder;

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

    public ExtensionServiceLoaderModule()
    {
        this(ExtensionServiceLoaderModule.class.getClassLoader());
    }

    public ExtensionServiceLoaderModule(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    @Override
    public void configure(Binder binder)
    {
        ServiceLoader<Extension> serviceLoader = ServiceLoader.load(Extension.class, classLoader);
        for (Extension extension : serviceLoader) {
            for (Module module : extension.getModules()) {
                module.configure(binder);
            }
        }
    }
}
