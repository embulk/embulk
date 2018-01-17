package org.embulk.cli.parse;

import org.apache.commons.cli.Option;

/**
 * AbstractHelpLineDefinition represents any one line in help messages in the command line parser.
 *
 * It is visible only in {@code org.embulk.cli.parse} because its interface communicates commons-cli objects.
 * Dependencies on third-party libraries are to be encapsulated.
 */
abstract class AbstractHelpLineDefinition {
    // Visible only in org.embulk.cli.parse to keep commons-cli segregated from other components.
    abstract Option getCliOption();
}
