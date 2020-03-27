package org.embulk.exec;

import com.google.inject.Binder;
import com.google.inject.Module;
import java.util.ServiceLoader;
import org.embulk.EmbulkSystemProperties;
import org.embulk.spi.Extension;

/**
 * ExtensionServiceLoaderModule loads Extensions using java.util.ServiceLoader
 * mechanism.
 * Jar packages providing an extension need to include
 * META-INF/services/org.embulk.exec.Extension file. Contents of the file is
 * one-line text of the extension class name (e.g. com.example.MyPluginSourceExtension).
 */
public class ExtensionServiceLoaderModule implements Module {
    private final ClassLoader classLoader;
    private final EmbulkSystemProperties embulkSystemProperties;

    public ExtensionServiceLoaderModule(final EmbulkSystemProperties embulkSystemProperties) {
        this(ExtensionServiceLoaderModule.class.getClassLoader(), embulkSystemProperties);
    }

    public ExtensionServiceLoaderModule(final ClassLoader classLoader, final EmbulkSystemProperties embulkSystemProperties) {
        this.classLoader = classLoader;
        this.embulkSystemProperties = embulkSystemProperties;
    }

    @Override
    public void configure(Binder binder) {
        ServiceLoader<Extension> serviceLoader = ServiceLoader.load(Extension.class, classLoader);
        for (Extension extension : serviceLoader) {
            for (Module module : extension.getModules(this.embulkSystemProperties)) {
                module.configure(binder);
            }
        }
    }
}
