package org.embulk.cli.parse;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.embulk.cli.EmbulkCommandLine;

public class EmbulkCommandLineParser {
    private EmbulkCommandLineParser(
            final String usage,
            final List<AbstractHelpLineDefinition> helpLineDefinitions,
            final int minArgs,
            final int maxArgs,
            final int width) {
        this.usage = usage;
        this.helpLineDefinitions = Collections.unmodifiableList(helpLineDefinitions);
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.width = width;

        this.cliOptions = new CliOptionsWithHelpMessages();

        final HashMap<Option, OptionDefinition> optionDefinitionFromCliOption = new HashMap<Option, OptionDefinition>();
        for (final AbstractHelpLineDefinition definition : this.helpLineDefinitions) {
            cliOptions.addOption(definition.getCliOption());
            if (definition instanceof OptionDefinition) {
                optionDefinitionFromCliOption.put(definition.getCliOption(), ((OptionDefinition) definition));
            }
        }
        this.optionDefinitionFromCliOption = optionDefinitionFromCliOption;
    }

    public static class Builder {
        private Builder() {
            this.mainUsage = null;
            this.additionalUsage = new StringBuilder();
            this.helpLineDefinitions = new ArrayList<AbstractHelpLineDefinition>();
            this.minArgs = 0;
            this.maxArgs = Integer.MAX_VALUE;
            this.width = 74;
        }

        public EmbulkCommandLineParser build() {
            return new EmbulkCommandLineParser(
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
            this.helpLineDefinitions.add(new HelpMessageLineDefinition(message));
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

    public EmbulkCommandLine parse(final List<String> argsEmbulk,
                                   final List<String> jrubyOptions,
                                   final PrintWriter helpPrintWriter,
                                   final PrintWriter errorPrintWriter)
            throws EmbulkCommandLineParseException, EmbulkCommandLineHelpRequired {
        final CommandLine cliCommandLine;
        try {
            cliCommandLine = new DefaultParser().parse(
                    this.cliOptions, argsEmbulk.toArray(new String[argsEmbulk.size()]));
        } catch (ParseException ex) {
            throw new EmbulkCommandLineParseException(ex.getMessage(), ex);
        }

        for (final Option cliOptionSpecified : cliCommandLine.getOptions()) {
            final OptionDefinition optionDefinitionSpecified =
                    this.optionDefinitionFromCliOption.get(cliOptionSpecified);

            if (optionDefinitionSpecified.printsHelp()) {
                throw new EmbulkCommandLineHelpRequired();
            }
        }

        final List<String> arguments = cliCommandLine.getArgList();
        if (arguments.size() < this.minArgs) {
            throw new EmbulkCommandLineParseException("Too few arguments.");
        }
        if (arguments.size() > this.maxArgs) {
            throw new EmbulkCommandLineParseException("Too many arguments;");
        }

        final EmbulkCommandLine.Builder commandLineBuilder = EmbulkCommandLine.builder();
        for (final String jrubyOption : jrubyOptions) {
            commandLineBuilder.addSystemConfig("jruby_command_line_options", jrubyOption);
        }
        commandLineBuilder.addArguments(arguments);
        for (final Option cliOptionSpecified : cliCommandLine.getOptions()) {
            final OptionDefinition optionDefinitionSpecified =
                    this.optionDefinitionFromCliOption.get(cliOptionSpecified);
            optionDefinitionSpecified.behave(commandLineBuilder, cliOptionSpecified.getValue());
        }

        return commandLineBuilder.build();
    }

    public final void printHelp(final PrintStream printStream) {
        this.printHelp(new PrintWriter(printStream));
    }

    public void printHelp(final PrintWriter printWriter) {
        final CliHelpFormatterWithHelpMessages helpFormatter = new CliHelpFormatterWithHelpMessages("Usage: ", 32);
        helpFormatter.printHelp(
                printWriter,  // PrintWriter pw
                this.width,  // int width
                this.usage,  // String cmdLineSyntax
                "",  // String header
                this.cliOptions,  // Options options
                4,  // int leftPad
                5,  // int descPad
                ""  // String footer
        );
    }

    private final String usage;
    private final List<AbstractHelpLineDefinition> helpLineDefinitions;
    private final int minArgs;
    private final int maxArgs;
    private final int width;
    private final CliOptionsWithHelpMessages cliOptions;
    private final HashMap<Option, OptionDefinition> optionDefinitionFromCliOption;
}
