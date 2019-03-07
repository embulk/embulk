package org.embulk.deps.cli;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.embulk.cli.EmbulkCommandLine;

public abstract class CliParser {
    public static class Builder {
        private Builder() {
            this.mainUsage = null;
            this.additionalUsage = new StringBuilder();
            this.helpLineDefinitions = new ArrayList<AbstractHelpLineDefinition>();
            this.minArgs = 0;
            this.maxArgs = Integer.MAX_VALUE;
            this.width = 74;
        }

        public CliParser build() {
            try {
                return CONSTRUCTOR.newInstance(
                        this.mainUsage + this.additionalUsage.toString(),
                        this.helpLineDefinitions,
                        this.minArgs,
                        this.maxArgs,
                        this.width);
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

        public Builder setMainUsage(final String mainUsage) {
            this.mainUsage = mainUsage;
            return this;
        }

        public Builder addUsage(final String line) {
            this.additionalUsage.append(System.getProperty("line.separator"));
            this.additionalUsage.append(line);
            return this;
        }

        public Builder addOptionDefinition(final OptionDefinition optionDefinition) {
            this.helpLineDefinitions.add(optionDefinition);
            return this;
        }

        public Builder addHelpMessageLine(final String message) {
            this.helpLineDefinitions.add(HelpMessageLineDefinition.create(message));
            return this;
        }

        public Builder setArgumentsRange(final int minArgs, final int maxArgs) {
            this.minArgs = minArgs;
            this.maxArgs = maxArgs;
            return this;
        }

        public Builder setWidth(final int width) {
            this.width = width;
            return this;
        }

        private String mainUsage;
        private final StringBuilder additionalUsage;
        private final ArrayList<AbstractHelpLineDefinition> helpLineDefinitions;
        private int minArgs;
        private int maxArgs;
        private int width;
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract EmbulkCommandLine parse(final List<String> argsEmbulk,
                                            final List<String> jrubyOptions,
                                            final PrintWriter helpPrintWriter,
                                            final PrintWriter errorPrintWriter)
            throws EmbulkCommandLineParseException, EmbulkCommandLineHelpRequired;

    public final void printHelp(final PrintStream printStream) {
        this.printHelp(new PrintWriter(printStream));
    }

    public abstract void printHelp(final PrintWriter printWriter);

    @SuppressWarnings("unchecked")
    private static Class<CliParser> loadImplClass() {
        try {
            return (Class<CliParser>) CLASS_LOADER.loadClass(CLASS_NAME);
        } catch (final ClassNotFoundException ex) {
            throw new LinkageError("Dependencies for Commons-CLI are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final ClassLoader CLASS_LOADER = CliParser.class.getClassLoader();
    private static final String CLASS_NAME = "org.embulk.deps.cli.CliParserImpl";

    static {
        final Class<CliParser> clazz = loadImplClass();
        try {
            CONSTRUCTOR = clazz.getConstructor(String.class, List.class, int.class, int.class, int.class);
        } catch (final NoSuchMethodException ex) {
            throw new LinkageError("Dependencies for Commons-CLI are not loaded correctly: " + CLASS_NAME, ex);
        }
    }

    private static final Constructor<CliParser> CONSTRUCTOR;
}
