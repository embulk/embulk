package org.embulk.deps.maven;

import java.io.FileNotFoundException;
import java.nio.file.Path;

public class MavenRepositoryNotFoundException extends FileNotFoundException {
    public MavenRepositoryNotFoundException(final Path givenPath,
                                            final Throwable cause) {
        super("Maven repository specified is not found at \"" + givenPath.toString() + "\".");
        this.initCause(cause);
    }

    public MavenRepositoryNotFoundException(final Path givenPath,
                                            final Path absolutePath,
                                            final Throwable cause) {
        super("Maven repository specified is not found at \"" + givenPath.toString()
                      + "\" (\"" + absolutePath.toString() + "\").");
        this.initCause(cause);
    }

    public MavenRepositoryNotFoundException(final String message,
                                            final Path givenPath,
                                            final Path absolutePath,
                                            final Throwable cause) {
        super("Maven repository specified is not found at \"" + givenPath.toString()
                      + "\" (\"" + absolutePath.toString() + "\"): " + message);
        this.initCause(cause);
    }
}
