package org.embulk.deps.cli;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.embulk.deps.DependencyCategory;
import org.embulk.deps.EmbulkDependencyClassLoaders;

// It is public just to be accessible from HelpMessageLineDefinitionImpl.
public abstract class HelpMessageLineDefinition extends AbstractHelpLineDefinition {
    static HelpMessageLineDefinition create(final String message) {
        try {
            return CONSTRUCTOR.newInstance(message);
        } catch (final IllegalAccessException | IllegalArgumentException | InstantiationException ex) {
            throw new LinkageError("Dependencies for Commons-CLI are not loaded correctly: " + CLASS_NAME, ex);
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

    @SuppressWarnings("unchecked")
    private static Class<HelpMessageLineDefinition> loadImplClass() {
        try {
            return (Class<HelpMessageLineDefinition>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Commons-CLI are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.of(DependencyCategory.CLI);
    private static final String CLASS_NAME = "org.embulk.deps.cli.HelpMessageLineDefinitionImpl";

    static {
        final Class<HelpMessageLineDefinition> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(String.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Commons-CLI are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<HelpMessageLineDefinition> CONSTRUCTOR;
}
