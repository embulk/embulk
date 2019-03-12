package org.embulk.deps.cli;

/**
 * AbstractHelpLineDefinition represents any one line in help messages in the command line parser.
 *
 * It is visible only in {@code org.embulk.deps.cli} because its interface communicates commons-cli objects.
 * Dependencies on third-party libraries are to be encapsulated.
 */
// It is public just to be accessed from CliParserImpl.
public abstract class AbstractHelpLineDefinition {
    // Visible only in org.embulk.deps.cli to keep commons-cli segregated from other components.
    // The return value should be org.apache.commons.cli.Option.
    // It is public just to be accessed from CliParserImpl.
    public abstract Object getCliOption();
}
