package org.embulk.cli.parse;

import org.apache.commons.cli.Option;
import org.embulk.cli.EmbulkCommandLine;

public class OptionDefinition extends AbstractHelpLineDefinition {
    private OptionDefinition(
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

    public static OptionDefinition defineOnlyLongOptionWithArgument(
            final String longOption,
            final String argumentNameDisplayed,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinition(null, longOption, true, argumentNameDisplayed, description, false, behavior);
    }

    public static OptionDefinition defineOnlyShortOptionWithArgument(
            final String shortOption,
            final String argumentNameDisplayed,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinition(shortOption, null, true, argumentNameDisplayed, description, false, behavior);
    }

    public static OptionDefinition defineOnlyShortOptionWithoutArgument(
            final String shortOption,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinition(shortOption, null, false, null, description, false, behavior);
    }

    public static OptionDefinition defineOptionWithoutArgument(
            final String shortOption,
            final String longOption,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinition(shortOption, longOption, false, null, description, false, behavior);
    }

    public static OptionDefinition defineOptionWithArgument(
            final String shortOption,
            final String longOption,
            final String argumentNameDisplayed,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinition(shortOption, longOption, true, argumentNameDisplayed, description, false, behavior);
    }

    public static OptionDefinition defineHelpOption(
            final String shortOption,
            final String longOption,
            final String description) {
        return new OptionDefinition(shortOption, longOption, false, null, description, true, new OptionBehavior() {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument) {
                }
            });
    }

    @Override
    final Option getCliOption() {
        return this.cliOption;
    }

    final void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
            throws EmbulkCommandLineParseException {
        this.behavior.behave(commandLineBuilder, argument);
    }

    final boolean printsHelp() {
        return this.printsHelp;
    }

    private final Option cliOption;
    private final boolean printsHelp;
    private final OptionBehavior behavior;
}
