package org.embulk.plugin.maven;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class TestMavenArtifactFinder {
    @Test
    public void testArtifactWithDependencies() throws Exception {
        final Path basePath = getMavenPath();
        final MavenArtifactFinder finder = MavenArtifactFinder.create(basePath);
        final MavenPluginPaths paths = finder.findMavenPluginJarsWithDirectDependencies(
                "org.embulk.example", "embulk-example-maven-artifact", null, "0.1.2");
        assertEquals(buildExpectedPath(basePath, GROUP_DIRECTORY, "embulk-example-maven-artifact", "0.1.2"),
                     paths.getPluginJarPath());

        // Confirming that nested dependencies (embulk-example-dependency-one's dependency) are not contained.
        assertEquals(1, paths.getPluginDependencyJarPaths().size());

        assertEquals(buildExpectedPath(basePath, GROUP_DIRECTORY, "embulk-example-dependency-one", "0.1.1"),
                     paths.getPluginDependencyJarPaths().get(0));
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

    private static final Path GROUP_DIRECTORY = Paths.get("org").resolve("embulk").resolve("example");
}
