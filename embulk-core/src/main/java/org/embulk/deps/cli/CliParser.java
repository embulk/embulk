package org.embulk.deps.cli;

import java.io.PrintStream;
import java.io.PrintWriter;
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
            return new CliParserImpl(
                    this.mainUsage + this.additionalUsage.toString(),
                    this.helpLineDefinitions,
                    this.minArgs,
                    this.maxArgs,
                    this.width);
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
}
