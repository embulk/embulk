package org.embulk.jruby;

public final class JRubyInvokeFailedException extends RuntimeException {  // Right to extend RuntimeException.
    public JRubyInvokeFailedException(final Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
