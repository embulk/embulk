package org.embulk.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmbulkArguments {
    private EmbulkArguments(final EmbulkSubcommand subcommand, final List<String> subcommandArguments) {
        this.subcommand = subcommand;
        this.subcommandArguments = Collections.unmodifiableList(subcommandArguments);
    }

    public static EmbulkArguments extract(final List<String> arguments)
            throws EmbulkCommandLineException {
        final ArrayList<String> subcommandArguments = new ArrayList<String>();

        EmbulkSubcommand subcommand = null;
        for (final String argument : arguments) {
            if (subcommand == null && (!argument.startsWith("-"))) {
                subcommand = EmbulkSubcommand.of(argument);
            } else if (subcommand == null && (argument.equals("-b") || argument.equals("--bundle"))) {
                throw new EmbulkCommandLineException("\"-b\" or \"--bundle\" before a subcommand is not supported.");
            } else if (subcommand == null && argument.equals("-version")) {
                return new EmbulkArguments(EmbulkSubcommand.VERSION_ERR, new ArrayList<String>());
            } else if (subcommand == null && argument.equals("--version")) {
                return new EmbulkArguments(EmbulkSubcommand.VERSION_OUT, new ArrayList<String>());
            } else {
                subcommandArguments.add(argument);
            }
        }

        return new EmbulkArguments(subcommand, subcommandArguments);
    }

    public final EmbulkSubcommand getSubcommand() {
        return this.subcommand;
    }

    public final List<String> getSubcommandArguments() {
        return this.subcommandArguments;
    }

    private final EmbulkSubcommand subcommand;
    private final List<String> subcommandArguments;
}
