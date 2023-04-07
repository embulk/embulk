package org.embulk.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.embulk.EmbulkDependencyClassLoader;

public abstract class ModelManagerDelegate {
    public static ModelManagerDelegate of() {
        try {
            return CONSTRUCTOR.newInstance();
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

    public abstract <T> T readObject(Class<T> valueType, String json);

    public abstract <T> T readObjectWithConfigSerDe(Class<T> valueType, String json);

    public abstract DataSource readObjectAsDataSource(String json);

    public abstract String writeObject(Object object);

    public abstract void validate(Object object);

    public abstract TaskReport newTaskReport();

    public abstract ConfigDiff newConfigDiff();

    public abstract ConfigSource newConfigSource();

    public abstract TaskSource newTaskSource();

    @SuppressWarnings("unchecked")
    private static Class<ModelManagerDelegate> loadImplClass() {
        try {
            return (Class<ModelManagerDelegate>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Jackson are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoader.get();
    private static final String CLASS_NAME = "org.embulk.deps.config.ModelManagerDelegateImpl";

    static {
        final Class<ModelManagerDelegate> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor();
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Jackson are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<ModelManagerDelegate> CONSTRUCTOR;
}
