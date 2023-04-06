package org.embulk.config;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.embulk.EmbulkDependencyClassLoader;

public abstract class YamlProcessor {
    public static YamlProcessor create(boolean withResolver) {
        try {
            return CONSTRUCTOR.newInstance(withResolver);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Yaml are not loaded correctly: " + CLASS_NAME, ex);
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

    /**
     * Parses YAML data in a String.
     *
     * @param data Yaml data.
     * @return Parsed object.
     */
    public abstract Object load(String data);

    /**
     * Parses YAML data in a Stream.
     *
     * @param data Yaml data.
     * @return Parsed object.
     */
    public abstract Object load(InputStream data);

    /**
     * Serializes an object into a YAML String.
     *
     * @param data Object.
     * @return YAML string.
     */
    public abstract String dump(Object data);

    @SuppressWarnings("unchecked")
    private static Class<YamlProcessor> loadImplClass() {
        try {
            return (Class<YamlProcessor>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Yaml are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoader.get();
    private static final String CLASS_NAME = "org.embulk.deps.config.YamlProcessorImpl";

    static {
        final Class<YamlProcessor> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(boolean.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Yaml are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<YamlProcessor> CONSTRUCTOR;
}
