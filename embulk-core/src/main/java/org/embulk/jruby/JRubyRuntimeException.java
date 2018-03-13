package org.embulk.jruby;

public final class JRubyRuntimeException extends RuntimeException {
    public JRubyRuntimeException(final Throwable cause) {
        super("Failed in JRuby runtime: " + cause.getMessage(), cause);
    }
}
