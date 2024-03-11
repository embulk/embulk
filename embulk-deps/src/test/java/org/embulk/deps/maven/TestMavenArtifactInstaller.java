package org.embulk.deps.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Test;

public class TestMavenArtifactInstaller {
    @Test
    public void testArtifactWithDependencies() throws Exception {
        final Path basePath = getMavenPath();
        final MavenArtifactInstallerImpl installer = new MavenArtifactInstallerImpl(basePath);
        installer.install("org.embulk:embulk-deps:0.11.0");
    }

    @Test
    public void testBuildRemoteRepositoryForCentral() throws Exception {
        final RemoteRepository repo1 = MavenArtifactInstallerImpl.buildRemoteRepositoryFromUrl("central");
        assertEquals("central", repo1.getId());
        assertEquals("default", repo1.getContentType());
        assertEquals("https://repo.maven.apache.org/maven2", repo1.getUrl());

        final RemoteRepository repo2 = MavenArtifactInstallerImpl.buildRemoteRepositoryFromUrl("https://repo.maven.apache.org/maven2");
        assertEquals("central", repo2.getId());
        assertEquals("default", repo2.getContentType());
        assertEquals("https://repo.maven.apache.org/maven2", repo2.getUrl());
    }

    @Test
    public void testBuildRemoteRepositoryForOthers() throws Exception {
        final RemoteRepository repo1 = MavenArtifactInstallerImpl.buildRemoteRepositoryFromUrl("https://jcenter.bintray.com/");
        assertEquals("", repo1.getId());
        assertEquals("", repo1.getContentType());
        assertEquals("https://jcenter.bintray.com/", repo1.getUrl());
    }

    @Test
    public void testBuildRemoteRepositoryForNull() throws Exception {
        try {
            MavenArtifactInstallerImpl.buildRemoteRepositoryFromUrl(null);
        } catch (final MalformedURLException ex) {
            return;
        }
        fail("Expected MalformedURLException for null.");
    }

    @Test
    public void testListRemoteRepositories1() throws Exception {
        final List<RemoteRepository> list = MavenArtifactInstallerImpl.listRemoteRepositories();
        assertEquals(1, list.size());
        assertEquals("central", list.get(0).getId());
        assertEquals("default", list.get(0).getContentType());
        assertEquals("https://repo.maven.apache.org/maven2", list.get(0).getUrl());
    }

    @Test
    public void testListRemoteRepositories2() throws Exception {
        final List<RemoteRepository> list = MavenArtifactInstallerImpl.listRemoteRepositories("https://jcenter.bintray.com/");
        assertEquals(1, list.size());
        assertEquals("", list.get(0).getId());
        assertEquals("", list.get(0).getContentType());
        assertEquals("https://jcenter.bintray.com/", list.get(0).getUrl());
    }

    @Test
    public void testListRemoteRepositories3() throws Exception {
        final List<RemoteRepository> list = MavenArtifactInstallerImpl.listRemoteRepositories(
                "central", "https://jcenter.bintray.com/");
        assertEquals(2, list.size());
        assertEquals("central", list.get(0).getId());
        assertEquals("default", list.get(0).getContentType());
        assertEquals("https://repo.maven.apache.org/maven2", list.get(0).getUrl());
        assertEquals("", list.get(1).getId());
        assertEquals("", list.get(1).getContentType());
        assertEquals("https://jcenter.bintray.com/", list.get(1).getUrl());
    }

    private Path getMavenPath() throws URISyntaxException {
        return Paths.get(this.getClass().getClassLoader().getResource("m2.test").toURI());
    }

    private Path buildExpectedPath(
            final Path basePath, final Path groupDirectory, final String artifactId, final String version) {
        return basePath
            .resolve(groupDirectory)
            .resolve(artifactId)
            .resolve(version)
            .resolve(artifactId + "-" + version + ".jar");
    }
}
