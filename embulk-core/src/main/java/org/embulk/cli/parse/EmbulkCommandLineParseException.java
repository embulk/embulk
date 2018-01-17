package org.embulk.cli.parse;

public final class EmbulkCommandLineParseException extends Exception {
    protected EmbulkCommandLineParseException() {
        super();
    }

    public EmbulkCommandLineParseException(final String message) {
        super(message);
    }

    public EmbulkCommandLineParseException(final Throwable cause) {
        super(cause);
    }

    public EmbulkCommandLineParseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
