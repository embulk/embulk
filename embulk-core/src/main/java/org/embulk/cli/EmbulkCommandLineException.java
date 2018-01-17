package org.embulk.cli;

public final class EmbulkCommandLineException extends RuntimeException {
    protected EmbulkCommandLineException() {
        super();
    }

    public EmbulkCommandLineException(final String message) {
        super(message);
    }

    public EmbulkCommandLineException(final Throwable cause) {
        super(cause);
    }

    public EmbulkCommandLineException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
