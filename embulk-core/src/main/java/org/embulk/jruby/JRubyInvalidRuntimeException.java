package org.embulk.jruby;

// TODO: Make it a checked exception (non RuntimeException).
public class JRubyInvalidRuntimeException extends RuntimeException {
    public JRubyInvalidRuntimeException(final String message) {
        super(message);
    }

    public JRubyInvalidRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
