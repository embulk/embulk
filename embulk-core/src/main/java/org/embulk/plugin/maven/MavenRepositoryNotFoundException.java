package org.embulk.plugin.maven;

import java.nio.file.Path;

public class MavenRepositoryNotFoundException extends Exception {
    public MavenRepositoryNotFoundException(final Path givenPath,
                                            final Throwable cause) {
        super("Maven repository specified is not found at \"" + givenPath.toString() + "\".", cause);
    }

    public MavenRepositoryNotFoundException(final Path givenPath,
                                            final Path absolutePath,
                                            final Throwable cause) {
        super("Maven repository specified is not found at \"" + givenPath.toString()
                      + "\" (\"" + absolutePath.toString() + "\").",
              cause);
    }

    public MavenRepositoryNotFoundException(final String message,
                                            final Path givenPath,
                                            final Path absolutePath,
                                            final Throwable cause) {
        super("Maven repository specified is not found at \"" + givenPath.toString()
                      + "\" (\"" + absolutePath.toString() + "\"): " + message,
              cause);
    }
}
