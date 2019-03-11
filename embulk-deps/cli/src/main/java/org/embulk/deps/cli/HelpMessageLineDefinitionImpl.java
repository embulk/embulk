package org.embulk.deps.cli;

import org.apache.commons.cli.Option;

// It is public just to be accessed from HelpMessageLineDefinition.
public final class HelpMessageLineDefinitionImpl extends HelpMessageLineDefinition {
    // It is public just to be called through getConstructor.
    public HelpMessageLineDefinitionImpl(final String message) {
        this.cliOption = new HelpMessageAsCliOption(message);
    }

    @Override
    public final Option getCliOption() {
        return this.cliOption;
    }

    private final Option cliOption;
}
