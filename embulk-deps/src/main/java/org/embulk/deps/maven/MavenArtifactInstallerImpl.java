/*
 * Copyright 2023 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.deps.maven;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.embulk.plugin.maven.MavenArtifactInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenArtifactInstallerImpl extends MavenArtifactInstaller {
    public MavenArtifactInstallerImpl(final Path localMavenRepositoryPath) throws FileNotFoundException {
        final Path absolutePath;
        try {
            absolutePath = localMavenRepositoryPath.normalize().toAbsolutePath();
        } catch (final IOError ex) {
            throw new MavenRepositoryNotFoundException(localMavenRepositoryPath, ex);
        } catch (final SecurityException ex) {
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

        this.givenLocalMavenRepositoryPath = localMavenRepositoryPath;
        this.absoluteLocalMavenRepositoryPath = absolutePath;

        this.repositorySystemSupplier = new InstallRepositorySystemSupplier();
        this.repositorySystem = this.repositorySystemSupplier.get();

        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        final LocalRepository localRepository = new LocalRepository(this.absoluteLocalMavenRepositoryPath.toString());
        session.setLocalRepositoryManager(
                this.repositorySystem.newLocalRepositoryManager(session, localRepository));
        this.repositorySystemSession = session;
    }

    @Override
    public final void install(
            final String coords,
            final String... repositoryUrls) throws IOException {
        final Artifact artifact = new DefaultArtifact(coords);

        final DependencyRequest dependencyRequest = buildDependencyRequest(artifact, repositoryUrls);

        final DependencyResult dependencyResult;
        try {
            dependencyResult = this.repositorySystem.resolveDependencies(
                    this.repositorySystemSession,
                    dependencyRequest);
        } catch (final DependencyResolutionException ex) {
            throw new MavenResolutionException(ex);
        }

        final List<ArtifactResult> artifactResults = dependencyResult.getArtifactResults();

        for (final ArtifactResult result : artifactResults) {
            final LocalArtifactResult localArtifactResult = result.getLocalArtifactResult();
            if (localArtifactResult.isAvailable()) {
                installLogger.info(
                        "Installed {} at {} (already)",
                        result.getArtifact(),
                        result.getArtifact().getFile());
            } else {
                installLogger.info(
                        "Installed {} at {}",
                        result.getArtifact(),
                        result.getArtifact().getFile());
            }
        }
    }

    private static DependencyRequest buildDependencyRequest(
            final Artifact artifact,
            final String... repositoryUrls) throws IOException {
        final List<RemoteRepository> repositories = listRemoteRepositories(repositoryUrls);

        final DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);

        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.RUNTIME));
        collectRequest.setRepositories(repositories);

        return new DependencyRequest(collectRequest, classpathFilter);
    }

    static List<RemoteRepository> listRemoteRepositories(final String... repositoryUrls) throws IOException {
        final ArrayList<RemoteRepository> repositories = new ArrayList<>();
        if (repositoryUrls.length == 0) {
            installLogger.info("No alternative remote Maven repositories are specified. Downloading artifacts from Maven Central.");
            repositories.add(MAVEN_CENTRAL);
        } else {
            for (final String url : repositoryUrls) {
                repositories.add(buildRemoteRepositoryFromUrl(url));
            }
            installLogger.info(
                    "Downloading artifacts from alternative remote Maven repositories: {}",
                    repositories.stream().map(RemoteRepository::getUrl).collect(Collectors.joining(", ")));
        }
        return Collections.unmodifiableList(repositories);
    }

    static RemoteRepository buildRemoteRepositoryFromUrl(final String url) throws IOException {
        if (url == null) {
            throw new MalformedURLException("null specified for remote repository.");
        }
        if (url.equalsIgnoreCase(MAVEN_CENTRAL.getId()) || url.equals(MAVEN_CENTRAL.getUrl())) {
            // "central" is interpreted as the Maven Central repository.
            return MAVEN_CENTRAL;
        }
        return new RemoteRepository.Builder(null, null, url).build();
    }

    private static class LoggingListener extends AbstractRepositoryListener {
        @Override
        public void artifactDownloaded(final RepositoryEvent event) {
            installLogger.info("Downloaded {} at {}", event.getArtifact().toString(), event.getFile());
        }

        @Override
        public void artifactDownloading(final RepositoryEvent event) {
            final ArtifactRepository repository = event.getRepository();
            if (repository instanceof RemoteRepository) {
                final RemoteRepository remoteRepository = (RemoteRepository) repository;
                installLogger.info("Downloading {} from {}", event.getArtifact().toString(), remoteRepository.getUrl());
            } else {
                installLogger.info("Downloading {} from {}", event.getArtifact().toString(), repository.toString());
            }
        }
    }

    private static class InstallRepositorySystemSupplier extends RepositorySystemSupplier {
        @Override
        protected Map<String, RepositoryListener> getRepositoryListeners() {
            final HashMap<String, RepositoryListener> listeners = new HashMap<>(super.getRepositoryListeners());
            listeners.put("embulk-install", new LoggingListener());
            return listeners;
        }
    }

    // https://maven.apache.org/ref/3.9.5/maven-core/apidocs/constant-values.html
    private static final RemoteRepository MAVEN_CENTRAL = new RemoteRepository.Builder(
            "central",  // "central"
            "default",
            "https://repo.maven.apache.org/maven2"  // "https://repo.maven.apache.org/maven2"
            ).build();

    private static final Logger installLogger = LoggerFactory.getLogger("install");

    // Paths are kept just for hinting in Exceptions.
    private final Path givenLocalMavenRepositoryPath;
    private final Path absoluteLocalMavenRepositoryPath;

    private final RepositorySystemSupplier repositorySystemSupplier;
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession repositorySystemSession;
}
