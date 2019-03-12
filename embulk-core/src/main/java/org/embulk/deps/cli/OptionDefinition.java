package org.embulk.deps.cli;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.embulk.cli.EmbulkCommandLine;
import org.embulk.deps.EmbulkDependencyClassLoaders;

public abstract class OptionDefinition extends AbstractHelpLineDefinition {
    public static OptionDefinition defineOnlyLongOptionWithArgument(
            final String longOption,
            final String argumentNameDisplayed,
            final String description,
            final OptionBehavior behavior) {
        return createOptionDefinitionImpl(null, longOption, true, argumentNameDisplayed, description, false, behavior);
    }

    public static OptionDefinition defineOnlyShortOptionWithArgument(
            final String shortOption,
            final String argumentNameDisplayed,
            final String description,
            final OptionBehavior behavior) {
        return createOptionDefinitionImpl(shortOption, null, true, argumentNameDisplayed, description, false, behavior);
    }

    public static OptionDefinition defineOnlyShortOptionWithoutArgument(
            final String shortOption,
            final String description,
            final OptionBehavior behavior) {
        return createOptionDefinitionImpl(shortOption, null, false, null, description, false, behavior);
    }

    public static OptionDefinition defineOptionWithoutArgument(
            final String shortOption,
            final String longOption,
            final String description,
            final OptionBehavior behavior) {
        return createOptionDefinitionImpl(shortOption, longOption, false, null, description, false, behavior);
    }

    public static OptionDefinition defineOptionWithArgument(
            final String shortOption,
            final String longOption,
            final String argumentNameDisplayed,
            final String description,
            final OptionBehavior behavior) {
        return createOptionDefinitionImpl(shortOption, longOption, true, argumentNameDisplayed, description, false, behavior);
    }

    public static OptionDefinition defineHelpOption(
            final String shortOption,
            final String longOption,
            final String description) {
        return createOptionDefinitionImpl(shortOption, longOption, false, null, description, true, new OptionBehavior() {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument) {
                }
            });
    }

    public abstract void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
            throws EmbulkCommandLineParseException;

    public abstract boolean printsHelp();

    private static OptionDefinition createOptionDefinitionImpl(
            final String shortOption,
            final String longOption,
            final boolean hasArgument,
            final String argumentNameDisplayed,
            final String description,
            final boolean printsHelp,
            final OptionBehavior behavior) {
        try {
            return CONSTRUCTOR.newInstance(
                    shortOption,
                    longOption,
                    hasArgument,
                    argumentNameDisplayed,
                    description,
                    printsHelp,
                    behavior);
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
    private static Class<OptionDefinition> loadImplClass() {
        try {
            return (Class<OptionDefinition>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Commons-CLI are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = EmbulkDependencyClassLoaders.ofCli();
    private static final String CLASS_NAME = "org.embulk.deps.cli.OptionDefinitionImpl";

    static {
        final Class<OptionDefinition> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(
                    String.class, String.class, boolean.class, String.class, String.class, boolean.class, OptionBehavior.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Commons-CLI are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<OptionDefinition> CONSTRUCTOR;
}
