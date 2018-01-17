package org.embulk.cli.parse;

import java.io.OutputStream;
import java.io.PrintWriter;
import org.embulk.cli.EmbulkCommandLine;

public abstract class OptionBehavior {
    public OptionBehavior() {
        this.helpWriter = SYSTEM_HELP_WRITER;
        this.errorWriter = SYSTEM_ERROR_WRITER;
    }

    public OptionBehavior(final OutputStream helpStream, final OutputStream errorStream) {
        this.helpWriter = new PrintWriter(helpStream, true);
        this.errorWriter = new PrintWriter(errorStream, true);
    }

    public abstract void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
            throws EmbulkCommandLineParseException;

    protected final PrintWriter helpWriter() {
        return this.helpWriter;
    }

    protected final PrintWriter errorWriter() {
        return this.errorWriter;
    }

    private static final PrintWriter SYSTEM_HELP_WRITER = new PrintWriter(System.out, true);
    private static final PrintWriter SYSTEM_ERROR_WRITER = new PrintWriter(System.err, true);

    private final PrintWriter helpWriter;
    private final PrintWriter errorWriter;
}
