package org.embulk.deps.cli;

import org.embulk.cli.EmbulkCommandLine;

public abstract class OptionDefinition extends AbstractHelpLineDefinition {
    public static OptionDefinition defineOnlyLongOptionWithArgument(
            final String longOption,
            final String argumentNameDisplayed,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinitionImpl(null, longOption, true, argumentNameDisplayed, description, false, behavior);
    }

    public static OptionDefinition defineOnlyShortOptionWithArgument(
            final String shortOption,
            final String argumentNameDisplayed,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinitionImpl(shortOption, null, true, argumentNameDisplayed, description, false, behavior);
    }

    public static OptionDefinition defineOnlyShortOptionWithoutArgument(
            final String shortOption,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinitionImpl(shortOption, null, false, null, description, false, behavior);
    }

    public static OptionDefinition defineOptionWithoutArgument(
            final String shortOption,
            final String longOption,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinitionImpl(shortOption, longOption, false, null, description, false, behavior);
    }

    public static OptionDefinition defineOptionWithArgument(
            final String shortOption,
            final String longOption,
            final String argumentNameDisplayed,
            final String description,
            final OptionBehavior behavior) {
        return new OptionDefinitionImpl(shortOption, longOption, true, argumentNameDisplayed, description, false, behavior);
    }

    public static OptionDefinition defineHelpOption(
            final String shortOption,
            final String longOption,
            final String description) {
        return new OptionDefinitionImpl(shortOption, longOption, false, null, description, true, new OptionBehavior() {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument) {
                }
            });
    }

    abstract void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
            throws EmbulkCommandLineParseException;

    abstract boolean printsHelp();
}
