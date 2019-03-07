package org.embulk.deps.cli;

import org.apache.commons.cli.Option;

class HelpMessageLineDefinitionImpl extends HelpMessageLineDefinition {
    // It is public just to be called through getConstructor.
    public HelpMessageLineDefinitionImpl(final String message) {
        this.cliOption = new HelpMessageAsCliOption(message);
    }

    @Override
    final Option getCliOption() {
        return this.cliOption;
    }

    private final Option cliOption;
}
