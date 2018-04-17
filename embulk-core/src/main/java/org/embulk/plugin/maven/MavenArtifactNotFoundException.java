package org.embulk.plugin.maven;

import java.nio.file.Path;
import org.eclipse.aether.artifact.Artifact;

public class MavenArtifactNotFoundException extends Exception {
    public MavenArtifactNotFoundException(final String groupId,
                                          final String artifactId,
                                          final String classifier,
                                          final String version,
                                          final Path givenRepositoryPath,
                                          final Path absoluteRepositoryPath,
                                          final Throwable cause) {
        super("Maven artifact \"" + groupId + ":" + artifactId + ":" + version
                      + (classifier != null ? (":" + classifier) : "") + "\" is not found: at \""
                      + givenRepositoryPath.toString() + "\" (\"" + absoluteRepositoryPath.toString() + "\").",
              cause);
    }

    public MavenArtifactNotFoundException(final Artifact artifact,
                                          final Path givenRepositoryPath,
                                          final Path absoluteRepositoryPath,
                                          final Throwable cause) {
        super("Maven artifact \"" + artifact.toString() + "\" is not found: at \""
                      + givenRepositoryPath.toString() + "\" (\"" + absoluteRepositoryPath.toString() + "\").",
              cause);
    }
}
