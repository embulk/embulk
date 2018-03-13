package org.embulk.jruby;

public final class JRubyNotLoadedException extends JRubyInvalidRuntimeException {
    public JRubyNotLoadedException() {
        super("JRuby runtime is not loaded successfully.");
    }
}
