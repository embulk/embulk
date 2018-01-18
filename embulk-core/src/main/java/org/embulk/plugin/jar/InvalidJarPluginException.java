package org.embulk.plugin.jar;

public class InvalidJarPluginException extends Exception {
    public InvalidJarPluginException(final String message) {
        super(message);
    }

    public InvalidJarPluginException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
