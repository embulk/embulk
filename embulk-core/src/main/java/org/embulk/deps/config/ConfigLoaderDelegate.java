package org.embulk.deps.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;
import org.embulk.config.ConfigSource;
import org.embulk.deps.EmbulkDependencyClassLoaders;

public abstract class ConfigLoaderDelegate {
    public static ConfigLoaderDelegate of(final ModelManagerDelegate model) {
        try {
            return CONSTRUCTOR.newInstance(model);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Jackson are not loaded correctly: " + CLASS_NAME, ex);
        } catch (final InvocationTargetException ex) {
            final Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            } else {
                throw new RuntimeException("Unexpected exception in creating: " + CLASS_NAME, ex);
            }
        }
    }

    public abstract ConfigSource newConfigSource();

    public abstract ConfigSource fromJsonString(String string);

    public abstract ConfigSource fromJsonFile(File file) throws IOException;

    public abstract ConfigSource fromJson(InputStream stream) throws IOException;

    public abstract ConfigSource fromYamlString(String string);

    public abstract ConfigSource fromYamlFile(File file) throws IOException;

    public abstract ConfigSource fromYaml(InputStream stream) throws IOException;

    public abstract ConfigSource fromPropertiesYamlLiteral(Properties props, String keyPrefix);

    public abstract ConfigSource fromPropertiesYamlLiteral(Map<String, String> props, String keyPrefix);

    public abstract ConfigSource fromPropertiesAsIs(Properties properties);

    @SuppressWarnings("unchecked")
    private static Class<ConfigLoaderDelegate> loadImplClass() {
        try {
            return (Class<ConfigLoaderDelegate>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Jackson are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.get();
    private static final String CLASS_NAME = "org.embulk.deps.config.ConfigLoaderDelegateImpl";

    static {
        final Class<ConfigLoaderDelegate> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(ModelManagerDelegate.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Jackson are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<ConfigLoaderDelegate> CONSTRUCTOR;
}
