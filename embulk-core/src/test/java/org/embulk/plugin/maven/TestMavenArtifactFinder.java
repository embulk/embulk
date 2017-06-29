package org.embulk.plugin.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class TestMavenArtifactFinder
{
    @Test
    public void testArtifacts() throws Exception
    {
        final Path basePath = getMavenPath();
        final MavenArtifactFinder finder = MavenArtifactFinder.create(basePath);
        assertEquals(buildExpectedPath(basePath, GROUP_DIRECTORY, "embulk-example-maven-artifact", "0.1.2"),
                     finder.findMavenArtifactJar("org.embulk.example", "embulk-example-maven-artifact", null, "0.1.2"));
    }

    private Path getMavenPath() throws URISyntaxException
    {
        return Paths.get(this.getClass().getClassLoader().getResource("m2.test").toURI());
    }

    private Path buildExpectedPath(
            final Path basePath, final Path groupDirectory, final String artifactId, final String version)
    {
        return basePath
            .resolve(groupDirectory)
            .resolve(artifactId)
            .resolve(version)
            .resolve(artifactId + "-" + version + ".jar");
    }

    private static final Path GROUP_DIRECTORY = Paths.get("org").resolve("embulk").resolve("example");
}
