package org.embulk.deps.cli;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.embulk.deps.EmbulkDependencyClassLoaders;

public abstract class VelocityEngineDelegate {
    public static VelocityEngineDelegate create() {
        try {
            return CONSTRUCTOR.newInstance();
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Velocity are not loaded correctly: " + CLASS_NAME, ex);
        } catch (final InvocationTargetException ex) {
            final Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            } else {
                throw new RuntimeException("Unexpected Exception in creating: " + CLASS_NAME, ex);
            }
        }
    }

    public abstract boolean evaluate(
            final Map<String, String> contextMap,
            final Writer writer,
            final String logTag,
            final Reader reader);

    @SuppressWarnings("unchecked")
    private static Class<VelocityEngineDelegate> loadImplClass() {
        try {
            return (Class<VelocityEngineDelegate>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Velocity are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.ofCli();
    private static final String CLASS_NAME = "org.embulk.deps.cli.VelocityEngineDelegateImpl";

    static {
        final Class<VelocityEngineDelegate> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor();
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Velocity are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<VelocityEngineDelegate> CONSTRUCTOR;
}
