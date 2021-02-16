/*
 * Copyright 2018 The Embulk project
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

package org.embulk.input.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.util.config.ConfigMapperFactory;
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
        final List<String> files = listFiles(task);

        // It intentionally tests in the platform-aware way, not in the platform-oblivious way.
        if (System.getProperty("os.name").contains("Windows")) {
            assertEquals(6, files.size());
            assertTrue(files.contains(buildPath("foofoo1")));
            assertTrue(files.contains(buildPath("FooFoo2")));
            assertTrue(files.contains(buildPath("foofoo4\\foo")));
            assertTrue(files.contains(buildPath("foofoo4\\bar")));
            assertTrue(files.contains(buildPath("fooFoo6\\foo")));
            assertTrue(files.contains(buildPath("fooFoo6\\bar")));
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
    public void testListFilesDots() throws IOException {
        // TODO: Mock the current directory.
        try {
            Files.createFile(Paths.get("file1"));
            Files.createFile(Paths.get("file2"));
            Files.createDirectory(Paths.get("dirA"));
            Files.createFile(Paths.get("dirA", "file3"));
            Files.createFile(Paths.get("dirA", "file4"));
            Files.createDirectory(Paths.get("dirB"));
            Files.createFile(Paths.get("dirB", "file5"));
            Files.createFile(Paths.get("dirB", "file6"));

            final LocalFileInputPlugin.PluginTask file1Task = buildRawTask("file1");
            final List<String> file1Files = listFiles(file1Task);
            assertEquals(1, file1Files.size());
            assertTrue(file1Files.contains("file1"));

            final LocalFileInputPlugin.PluginTask dotSlashFile1Task = buildRawTask("." + File.separator + "file1");
            final List<String> dotSlashFile1Files = listFiles(dotSlashFile1Task);
            assertEquals(1, dotSlashFile1Files.size());
            assertTrue(dotSlashFile1Files.contains("." + File.separator + "file1"));

            final LocalFileInputPlugin.PluginTask fileTask = buildRawTask("file");
            final List<String> fileFiles = listFiles(fileTask);
            assertEquals(2, fileFiles.size());
            assertTrue(fileFiles.contains("file1"));
            assertTrue(fileFiles.contains("file2"));

            final LocalFileInputPlugin.PluginTask dotSlashFileTask = buildRawTask("." + File.separator + "file");
            final List<String> dotSlashFileFiles = listFiles(dotSlashFileTask);
            assertEquals(2, dotSlashFileFiles.size());
            assertTrue(dotSlashFileFiles.contains("." + File.separator + "file1"));
            assertTrue(dotSlashFileFiles.contains("." + File.separator + "file2"));

            final LocalFileInputPlugin.PluginTask dirATask = buildRawTask("dirA");
            final List<String> dirAFiles = listFiles(dirATask);
            assertEquals(2, dirAFiles.size());
            assertTrue(dirAFiles.contains("dirA" + File.separator + "file3"));
            assertTrue(dirAFiles.contains("dirA" + File.separator + "file4"));

            final LocalFileInputPlugin.PluginTask dotSlashDirATask = buildRawTask("." + File.separator + "dirA");
            final List<String> dotSlashDirAFiles = listFiles(dotSlashDirATask);
            assertEquals(2, dotSlashDirAFiles.size());
            assertTrue(dotSlashDirAFiles.contains("." + File.separator + "dirA" + File.separator + "file3"));
            assertTrue(dotSlashDirAFiles.contains("." + File.separator + "dirA" + File.separator + "file4"));

            final LocalFileInputPlugin.PluginTask dirTask = buildRawTask("dir");
            final List<String> dirFiles = listFiles(dirTask);
            assertEquals(4, dirFiles.size());
            assertTrue(dirFiles.contains("dirA" + File.separator + "file3"));
            assertTrue(dirFiles.contains("dirA" + File.separator + "file4"));
            assertTrue(dirFiles.contains("dirB" + File.separator + "file5"));
            assertTrue(dirFiles.contains("dirB" + File.separator + "file6"));

            final LocalFileInputPlugin.PluginTask dotSlashDirTask = buildRawTask("." + File.separator + "dir");
            final List<String> dotSlashDirFiles = listFiles(dotSlashDirTask);
            assertEquals(4, dotSlashDirFiles.size());
            assertTrue(dotSlashDirFiles.contains("." + File.separator + "dirA" + File.separator + "file3"));
            assertTrue(dotSlashDirFiles.contains("." + File.separator + "dirA" + File.separator + "file4"));
            assertTrue(dotSlashDirFiles.contains("." + File.separator + "dirB" + File.separator + "file5"));
            assertTrue(dotSlashDirFiles.contains("." + File.separator + "dirB" + File.separator + "file6"));

            final LocalFileInputPlugin.PluginTask dotSlashTask = buildRawTask("." + File.separator + "");
            final List<String> dotSlashFiles = listFiles(dotSlashTask);
            assertTrue(6 <= dotSlashFiles.size());  // Other files and directories exist.
            assertTrue(dotSlashFiles.contains("." + File.separator + "file1"));
            assertTrue(dotSlashFiles.contains("." + File.separator + "file2"));
            assertTrue(dotSlashFiles.contains("." + File.separator + "dirA" + File.separator + "file3"));
            assertTrue(dotSlashFiles.contains("." + File.separator + "dirA" + File.separator + "file4"));
            assertTrue(dotSlashFiles.contains("." + File.separator + "dirB" + File.separator + "file5"));
            assertTrue(dotSlashFiles.contains("." + File.separator + "dirB" + File.separator + "file6"));

            final LocalFileInputPlugin.PluginTask dotTask = buildRawTask(".");
            final List<String> dotFiles = listFiles(dotTask);
            assertTrue(6 <= dotFiles.size());  // Other files and directories exist.
            assertTrue(dotFiles.contains("." + File.separator + "file1"));
            assertTrue(dotFiles.contains("." + File.separator + "file2"));
            assertTrue(dotFiles.contains("." + File.separator + "dirA" + File.separator + "file3"));
            assertTrue(dotFiles.contains("." + File.separator + "dirA" + File.separator + "file4"));
            assertTrue(dotFiles.contains("." + File.separator + "dirB" + File.separator + "file5"));
            assertTrue(dotFiles.contains("." + File.separator + "dirB" + File.separator + "file6"));
        } finally {
            Files.deleteIfExists(Paths.get("dirB", "file6"));
            Files.deleteIfExists(Paths.get("dirB", "file5"));
            Files.deleteIfExists(Paths.get("dirB"));
            Files.deleteIfExists(Paths.get("dirA", "file4"));
            Files.deleteIfExists(Paths.get("dirA", "file3"));
            Files.deleteIfExists(Paths.get("dirA"));
            Files.deleteIfExists(Paths.get("file2"));
            Files.deleteIfExists(Paths.get("file1"));
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
        this.workdir.newFile("directory1/bar");
        this.workdir.newFolder("directory2");
        this.workdir.newFile("directory2/bar");
        final List<String> files = listFiles(task);

        // It intentionally tests in the platform-aware way, not in the platform-oblivious way.
        if (System.getProperty("os.name").contains("Windows")) {
            assertEquals(3, files.size());
            assertTrue(files.contains(buildPath("directory1\\foo1")));
            assertTrue(files.contains(buildPath("directory1\\foo2")));
            assertTrue(files.contains(buildPath("directory1\\Foo3")));
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
        this.workdir.newFile("directory1/bar");
        this.workdir.newFolder("directory2");
        this.workdir.newFile("directory2/bar");
        final List<String> files = listFiles(task);

        // It intentionally tests in the platform-aware way, not in the platform-oblivious way.
        if (System.getProperty("os.name").contains("Windows")) {
            assertEquals(3, files.size());
            assertTrue(files.contains(buildPath("directory1\\foo1")));
            assertTrue(files.contains(buildPath("directory1\\foo2")));
            assertTrue(files.contains(buildPath("directory1\\Foo3")));
        } else if (System.getProperty("os.name").contains("Mac OS")) {
            assertEquals(0, files.size());
        } else {
            assertEquals(0, files.size());
        }
    }

    private static List<String> listFiles(final LocalFileInputPlugin.PluginTask task) {
        return LocalFileInputPlugin.listFilesForTesting(task);
    }

    private LocalFileInputPlugin.PluginTask buildRawTask(
            final String pathPrefix) {
        return this.buildRawTask(pathPrefix, null, null);
    }

    private LocalFileInputPlugin.PluginTask buildRawTask(
            final String pathPrefix,
            final String lastPath,
            final Boolean followSymlinks) {
        final ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource();
        config.set("path_prefix", pathPrefix);
        if (lastPath != null) {
            config.set("last_path", Optional.of(lastPath));
        }
        if (followSymlinks != null) {
            config.set("follow_symlinks", followSymlinks);
        }
        return CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, LocalFileInputPlugin.PluginTask.class);
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
        final ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource();
        config.set("path_prefix", pathPrefix);
        if (lastPath != null) {
            config.set("last_path", Optional.of(lastPath));
        }
        if (followSymlinks != null) {
            config.set("follow_symlinks", followSymlinks);
        }
        return CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, LocalFileInputPlugin.PluginTask.class);
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

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();
}
