package org.embulk.deps.cli;

import org.apache.commons.cli.Option;
import org.embulk.cli.EmbulkCommandLine;

// It is public just to be accessed from OptionDefinition.
public final class OptionDefinitionImpl extends OptionDefinition {
    // It is public just to be called through getConstructor.
    public OptionDefinitionImpl(
            final String shortOption,
            final String longOption,
            final boolean hasArgument,
            final String argumentNameDisplayed,
            final String description,
            final boolean printsHelp,
            final OptionBehavior behavior) {
        final Option.Builder builder;

        if (shortOption == null) {
            builder = Option.builder();
        } else {
            builder = Option.builder(shortOption);
        }

        if (longOption != null) {
            builder.longOpt(longOption);
        }

        if (hasArgument) {
            builder.hasArg(true);
            builder.argName(argumentNameDisplayed);
        }
        if (description != null) {
            builder.desc(description);
        }
        this.cliOption = builder.build();
        this.printsHelp = printsHelp;
        this.behavior = behavior;
    }

    @Override
    public final Object getCliOption() {
        return this.cliOption;
    }

    @Override
    public final void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
            throws EmbulkCommandLineParseException {
        this.behavior.behave(commandLineBuilder, argument);
    }

    @Override
    public final boolean printsHelp() {
        return this.printsHelp;
    }

    private final Option cliOption;
    private final boolean printsHelp;
    private final OptionBehavior behavior;
}
