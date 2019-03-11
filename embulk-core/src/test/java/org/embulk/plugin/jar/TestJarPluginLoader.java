package org.embulk.plugin.jar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.embulk.EmbulkTestRuntime;
import org.embulk.plugin.PluginClassLoader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test cases in this class depend on JARs built by "test-helpers" project.
 *
 * See "deployJarsForEmbulkTestMavenPlugin" task in test-helpers/build.gradle for more details
 */
public class TestJarPluginLoader {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public EmbulkTestRuntime testRuntime = new EmbulkTestRuntime();

    @Test
    public void testLoadPluginClassForFlatJar() throws Exception {
        final Path pluginJarPath = BUILT_JARS_DIR.resolve("embulk-test-maven-plugin-flat.jar");
        final Path dependencyJarPath = BUILT_JARS_DIR.resolve("embulk-test-maven-plugin-deps.jar");

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

        final ClassLoader classLoader = loadedClass.getClassLoader();
        assertTrue(classLoader instanceof PluginClassLoader);

        verifyMainClass(loadedClass);

        assertEquals("Hello", readResource(classLoader, "embulk-test-maven-plugin/main.txt"));
        assertEquals("World", readResource(classLoader, "embulk-test-maven-plugin/deps.txt"));
    }

    // TODO: Remove the feature soon (see: https://github.com/embulk/embulk/issues/1110)
    @Test
    public void testLoadPluginClassForNestedJar() throws Exception {
        final Path pluginJarPath = BUILT_JARS_DIR.resolve("embulk-test-maven-plugin-nested.jar");

        final Class<?> loadedClass;
        try (final JarPluginLoader loader = JarPluginLoader.load(
                pluginJarPath,
                Collections.emptyList(),
                testRuntime.getPluginClassLoaderFactory())) {
            assertEquals(0, loader.getPluginSpiVersion());
            loadedClass = loader.getPluginMainClass();
        }

        final ClassLoader classLoader = loadedClass.getClassLoader();
        assertTrue(classLoader instanceof PluginClassLoader);

        verifyMainClass(loadedClass);

        // Probably a bug of ClassLoader, but as the nested style will be removed soon, so keep the behavior as-is.
        assertNull(classLoader.getResourceAsStream("embulk-test-maven-plugin/main.txt"));
        assertNull(classLoader.getResourceAsStream("embulk-test-maven-plugin/deps.txt"));
    }

    private static void verifyMainClass(Class<?> mainClass) throws Exception {
        final Object instanceObject = mainClass.newInstance();
        assertEquals("org.embulk.plugin.jar.ExampleJarSpiV0", instanceObject.getClass().getName());
        assertEquals("foobar", ((Callable) instanceObject).call());

        final Object dependencyInstance = ((Supplier) instanceObject).get();
        assertEquals("org.embulk.plugin.jar.ExampleDependencyJar", dependencyInstance.getClass().getName());
        assertEquals("hoge", ((Callable) dependencyInstance).call());
    }

    private static String readResource(ClassLoader classLoader, String name) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(classLoader.getResourceAsStream(name)))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private static Path EMBULK_ROOT_DIR = Paths.get(System.getProperty("user.dir")).getParent();

    private static Path BUILT_JARS_DIR = EMBULK_ROOT_DIR.resolve("test-helpers").resolve("build").resolve("embulk-test-maven-plugin");
}
