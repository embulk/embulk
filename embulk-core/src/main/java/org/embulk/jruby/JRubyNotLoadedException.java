package org.embulk.jruby;

// TODO: Make it a checked exception (non RuntimeException).
public final class JRubyNotLoadedException extends RuntimeException {
    public JRubyNotLoadedException() {
        super("JRuby runtime is not loaded successfully.");
    }

    public JRubyNotLoadedException(final String message) {
        super(message);
    }

    public JRubyNotLoadedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public JRubyNotLoadedException(final Throwable cause) {
        super("JRuby runtime is not loaded successfully.", cause);
    }
}
