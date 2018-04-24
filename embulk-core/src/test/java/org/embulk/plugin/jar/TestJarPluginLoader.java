package org.embulk.plugin.jar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import org.embulk.EmbulkTestRuntime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestJarPluginLoader {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public EmbulkTestRuntime testRuntime = new EmbulkTestRuntime();

    @Test
    public void test() throws Exception {
        final Path temporaryDirectory = createTemporaryJarDirectory();

        final Path pluginJarPath = createTemporaryPluginJarFile(temporaryDirectory);
        final JarBuilder pluginJarBuilder = new JarBuilder();
        pluginJarBuilder.addManifestV0(ExampleJarSpiV0.class.getName());
        pluginJarBuilder.addClass(ExampleJarSpiV0.class);
        pluginJarBuilder.build(pluginJarPath);

        final Path dependencyJarPath = createTemporaryDependencyJarFile(temporaryDirectory);
        final JarBuilder dependencyJarBuilder = new JarBuilder();
        pluginJarBuilder.addClass(ExampleDependencyJar.class);
        pluginJarBuilder.build(dependencyJarPath);

        final ArrayList<Path> dependencyJarPaths = new ArrayList<>();
        dependencyJarPaths.add(dependencyJarPath);

        final Class<?> loadedClass;
        try (final JarPluginLoader loader = JarPluginLoader.load(
                 pluginJarPath,
                 Collections.unmodifiableList(dependencyJarPaths),
                 testRuntime.getPluginClassLoaderFactory())) {
            assertEquals(0, loader.getPluginSpiVersion());
            loadedClass = loader.getPluginMainClass();
        }

        final Object instanceObject = loadedClass.newInstance();
        assertTrue(instanceObject instanceof ExampleJarSpiV0);

        final ExampleJarSpiV0 instance = (ExampleJarSpiV0) instanceObject;
        assertEquals("foobar", instance.getTestString());

        final ExampleDependencyJar dependencyInstance = instance.getDependencyObject();
        assertEquals("hoge", dependencyInstance.getTestDependencyString());
    }

    private Path createTemporaryPluginJarFile(final Path temporaryDirectoryPath) throws Exception {
        return Files.createTempFile(temporaryDirectoryPath, "testplugin", ".jar");
    }

    private Path createTemporaryDependencyJarFile(final Path temporaryDirectoryPath) throws Exception {
        return Files.createTempFile(temporaryDirectoryPath, "dependency", ".jar");
    }

    private Path createTemporaryJarDirectory() throws Exception {
        final String temporaryDirectoryString =
                System.getProperty("org.embulk.plugin.jar.TestJarPluginLoader.temporaryDirectory");

        if (temporaryDirectoryString == null) {
            return temporaryFolder.getRoot().toPath();
        } else {
            return Paths.get(temporaryDirectoryString);
        }
    }
}
