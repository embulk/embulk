package org.embulk.deps.cli;

abstract class HelpMessageLineDefinition extends AbstractHelpLineDefinition {
    static HelpMessageLineDefinition create(final String message) {
        return new HelpMessageLineDefinitionImpl(message);
    }
}
