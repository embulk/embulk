package org.embulk.plugin.maven;

import java.io.IOError;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public class MavenArtifactFinder
{
    private MavenArtifactFinder(final Path givenLocalMavenRepositoryPath,
                                final Path absoluteLocalMavenRepositoryPath,
                                final RepositorySystem repositorySystem,
                                final RepositorySystemSession repositorySystemSession)
    {
        this.givenLocalMavenRepositoryPath = givenLocalMavenRepositoryPath;
        this.absoluteLocalMavenRepositoryPath = absoluteLocalMavenRepositoryPath;
        this.repositorySystem = repositorySystem;
        this.repositorySystemSession = repositorySystemSession;
    }

    public static MavenArtifactFinder create(final Path localMavenRepositoryPath)
        throws MavenRepositoryNotFoundException
    {
        final Path absolutePath;
        try {
            absolutePath = localMavenRepositoryPath.normalize().toAbsolutePath();
        }
        catch (IOError ex) {
            throw new MavenRepositoryNotFoundException(localMavenRepositoryPath, ex);
        }
        catch (SecurityException ex) {
            throw new MavenRepositoryNotFoundException(localMavenRepositoryPath, ex);
        }

        if (!Files.exists(absolutePath)) {
            throw new MavenRepositoryNotFoundException(localMavenRepositoryPath,
                                                       absolutePath,
                                                       new NoSuchFileException(absolutePath.toString()));
        }
        if (!Files.isDirectory(absolutePath)) {
            throw new MavenRepositoryNotFoundException(localMavenRepositoryPath,
                                                       absolutePath,
                                                       new NotDirectoryException(absolutePath.toString()));
        }

        final RepositorySystem repositorySystem = createRepositorySystem();

        return new MavenArtifactFinder(localMavenRepositoryPath,
                                       absolutePath,
                                       repositorySystem,
                                       createRepositorySystemSession(repositorySystem, absolutePath));
    }

    public final Path findMavenArtifactJar(
            final String groupId,
            final String artifactId,
            final String classifier,
            final String version)
        throws MavenArtifactNotFoundException
    {
        return findMavenArtifact(groupId, artifactId, classifier, "jar", version);
    }

    public Path findMavenArtifact(
            final String groupId,
            final String artifactId,
            final String classifier,
            final String extension,
            final String version)
        throws MavenArtifactNotFoundException
    {
        final ArtifactResult result;
        try {
            result = resolveMavenArtifact(groupId, artifactId, classifier, extension, version);
        }
        catch (ArtifactResolutionException ex) {
            throw new MavenArtifactNotFoundException(groupId, artifactId, classifier, version,
                                                     this.givenLocalMavenRepositoryPath,
                                                     this.absoluteLocalMavenRepositoryPath,
                                                     ex);
        }
        return result.getArtifact().getFile().toPath();
    }

    private ArtifactResult resolveMavenArtifact(
            final String groupId,
            final String artifactId,
            final String classifier,
            final String extension,
            final String version)
        throws ArtifactResolutionException
    {
        // |classifier| can be null for |org.eclipse.aether.artifact.DefaultArtifact|.
        final ArtifactRequest artifactRequest = new ArtifactRequest().setArtifact(
            new DefaultArtifact(groupId, artifactId, classifier, extension, version));

        return this.repositorySystem.resolveArtifact(this.repositorySystemSession, artifactRequest);
    }

    private static RepositorySystem createRepositorySystem()
    {
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession createRepositorySystemSession(
            final RepositorySystem repositorySystem, final Path localRepositoryPath)
    {
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        final LocalRepository repository = new LocalRepository(localRepositoryPath.toString());
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, repository));
        return session;
    }

    // Paths are kept just for hinting in Exceptions.
    private final Path givenLocalMavenRepositoryPath;
    private final Path absoluteLocalMavenRepositoryPath;

    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession repositorySystemSession;
}
