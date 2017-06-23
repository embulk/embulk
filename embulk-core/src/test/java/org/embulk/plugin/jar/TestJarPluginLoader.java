package org.embulk.plugin.jar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.embulk.EmbulkTestRuntime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestJarPluginLoader
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public EmbulkTestRuntime testRuntime = new EmbulkTestRuntime();

    @Test
    public void test() throws Exception
    {
        final Path jarPath = createTemporaryJarFile();

        final JarBuilder jarBuilder = new JarBuilder();
        jarBuilder.addManifestV0(ExampleJarSpiV0.class.getName());
        jarBuilder.addClass(ExampleJarSpiV0.class);
        jarBuilder.build(jarPath);

        final Class<?> loadedClass;
        try (final JarPluginLoader loader = JarPluginLoader.load(jarPath, testRuntime.getPluginClassLoaderFactory())) {
            assertEquals(0, loader.getPluginSpiVersion());
            loadedClass = loader.getPluginMainClass();
        }

        final Object instanceObject = loadedClass.newInstance();
        assertTrue(instanceObject instanceof ExampleJarSpiV0);

        final ExampleJarSpiV0 instance = (ExampleJarSpiV0) instanceObject;
        assertEquals("foobar", instance.getTestString());
    }

    private Path createTemporaryJarFile() throws Exception
    {
        final String temporaryDirectoryString =
            System.getProperty("org.embulk.plugin.jar.TestJarPluginLoader.temporaryDirectory");

        final Path temporaryDirectoryPath;
        if (temporaryDirectoryString == null) {
            temporaryDirectoryPath = temporaryFolder.getRoot().toPath();
        }
        else {
            temporaryDirectoryPath = Paths.get(temporaryDirectoryString);
        }

        return Files.createTempFile(temporaryDirectoryPath, "testplugin", ".jar");
    }
}
