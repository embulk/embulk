package org.embulk.standards;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Exec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests LocalFileInputPlugin.
 */
public class TestLocalFileInputPlugin {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Rule
    public TemporaryFolder workdir = new TemporaryFolder();

    @Test
    public void testListFiles() throws IOException {
        final LocalFileInputPlugin.PluginTask task = buildTask("foofoo");
        this.workdir.newFile("foofoo1");
        this.workdir.newFile("FooFoo2");
        this.workdir.newFile("barbar3");
        this.workdir.newFolder("foofoo4");
        this.workdir.newFile("foofoo4/foo");
        this.workdir.newFile("foofoo4/bar");
        this.workdir.newFolder("barbar5");
        this.workdir.newFile("barbar5/foo");
        this.workdir.newFile("barbar5/bar");
        this.workdir.newFolder("fooFoo6");
        this.workdir.newFile("fooFoo6/foo");
        this.workdir.newFile("fooFoo6/bar");
        final LocalFileInputPlugin plugin = new LocalFileInputPlugin();
        final List<String> files = plugin.listFiles(task);

        // It intentionally tests in the platform-aware way, not in the platform-oblivious way.
        if (System.getProperty("os.name").contains("Windows")) {
            assertEquals(3, files.size());
            assertTrue(files.contains(buildPath("foofoo1")));
            assertTrue(files.contains(buildPath("foofoo4\\foo")));
            assertTrue(files.contains(buildPath("foofoo4\\bar")));
        } else if (System.getProperty("os.name").contains("Mac OS")) {
            assertEquals(3, files.size());
            assertTrue(files.contains(buildPath("foofoo1")));
            assertTrue(files.contains(buildPath("foofoo4/foo")));
            assertTrue(files.contains(buildPath("foofoo4/bar")));
        } else {
            assertEquals(3, files.size());
            assertTrue(files.contains(buildPath("foofoo1")));
            assertTrue(files.contains(buildPath("foofoo4/foo")));
            assertTrue(files.contains(buildPath("foofoo4/bar")));
        }
    }

    @Test
    public void testListFilesWithSameCaseDirectoryPrefix() throws IOException {
        final LocalFileInputPlugin.PluginTask task;
        if (System.getProperty("os.name").contains("Windows")) {
            task = buildTask("directory1\\foo");
        } else {
            task = buildTask("directory1/foo");
        }

        this.workdir.newFile("foo");
        this.workdir.newFolder("directory1");
        this.workdir.newFile("directory1/foo1");
        this.workdir.newFile("directory1/foo2");
        this.workdir.newFile("directory1/Foo3");
        this.workdir.newFolder("directory2");
        this.workdir.newFile("directory2/bar");
        final LocalFileInputPlugin plugin = new LocalFileInputPlugin();
        final List<String> files = plugin.listFiles(task);

        // It intentionally tests in the platform-aware way, not in the platform-oblivious way.
        if (System.getProperty("os.name").contains("Windows")) {
            assertEquals(2, files.size());
            assertTrue(files.contains(buildPath("directory1\\foo1")));
            assertTrue(files.contains(buildPath("directory1\\foo2")));
        } else if (System.getProperty("os.name").contains("Mac OS")) {
            assertEquals(2, files.size());
            assertTrue(files.contains(buildPath("directory1/foo1")));
            assertTrue(files.contains(buildPath("directory1/foo2")));
        } else {
            assertEquals(2, files.size());
            assertTrue(files.contains(buildPath("directory1/foo1")));
            assertTrue(files.contains(buildPath("directory1/foo2")));
        }
    }

    @Test
    public void testListFilesWithDifferentCaseDirectoryPrefix() throws IOException {
        final LocalFileInputPlugin.PluginTask task;
        if (System.getProperty("os.name").contains("Windows")) {
            task = buildTask("Directory1\\foo");
        } else {
            task = buildTask("Directory1/foo");
        }

        if ((!System.getProperty("os.name").contains("Windows"))
                && (!System.getProperty("os.name").contains("Mac OS"))) {
            this.workdir.newFolder("Directory1");
        }
        this.workdir.newFile("foo");
        this.workdir.newFolder("directory1");
        this.workdir.newFile("directory1/foo1");
        this.workdir.newFile("directory1/foo2");
        this.workdir.newFile("directory1/Foo3");
        this.workdir.newFolder("directory2");
        this.workdir.newFile("directory2/bar");
        final LocalFileInputPlugin plugin = new LocalFileInputPlugin();
        final List<String> files = plugin.listFiles(task);

        // It intentionally tests in the platform-aware way, not in the platform-oblivious way.
        if (System.getProperty("os.name").contains("Windows")) {
            assertEquals(2, files.size());
            assertTrue(files.contains(buildPath("Directory1\\foo1")));
            assertTrue(files.contains(buildPath("Directory1\\foo2")));
        } else if (System.getProperty("os.name").contains("Mac OS")) {
            assertEquals(2, files.size());
            assertTrue(files.contains(buildPath("Directory1/foo1")));
            assertTrue(files.contains(buildPath("Directory1/foo2")));
        } else {
            assertEquals(0, files.size());
        }
    }

    private LocalFileInputPlugin.PluginTask buildTask(
            final String subPathPrefix) {
        return this.buildTask(subPathPrefix, null, null);
    }

    private LocalFileInputPlugin.PluginTask buildTask(
            final String subPathPrefix,
            final String lastPath,
            final Boolean followSymlinks) {
        final String pathPrefix = this.buildPath(subPathPrefix);
        final ConfigSource config = Exec.newConfigSource();
        config.set("path_prefix", pathPrefix);
        if (lastPath != null) {
            config.set("last_path", Optional.of(lastPath));
        }
        if (followSymlinks != null) {
            config.set("follow_symlinks", followSymlinks);
        }
        return config.loadConfig(LocalFileInputPlugin.PluginTask.class);
    }

    private String buildPath(final String subPath) {
        final String workdirPath = this.workdir.getRoot().getPath();
        final StringBuilder pathPrefixBuilder = new StringBuilder(workdirPath);
        if (!workdirPath.endsWith(File.separator) && !subPath.startsWith(File.separator)) {
            pathPrefixBuilder.append(File.separator);
        }
        pathPrefixBuilder.append(subPath);
        return pathPrefixBuilder.toString();
    }
}
