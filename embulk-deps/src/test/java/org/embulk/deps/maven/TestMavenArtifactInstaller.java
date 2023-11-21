package org.embulk.deps.maven;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class TestMavenArtifactInstaller {
    @Test
    public void testArtifactWithDependencies() throws Exception {
        final Path basePath = getMavenPath();
        final MavenArtifactInstallerImpl installer = new MavenArtifactInstallerImpl(basePath);
        installer.install("org.embulk:embulk-deps:0.11.0");
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
