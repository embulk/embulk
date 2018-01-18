package org.embulk.cli.parse;

import org.apache.commons.cli.Option;

class HelpMessageLineDefinition extends AbstractHelpLineDefinition {
    HelpMessageLineDefinition(final String message) {
        this.cliOption = new HelpMessageAsCliOption(message);
    }

    @Override
    final Option getCliOption() {
        return this.cliOption;
    }

    private final Option cliOption;
}
