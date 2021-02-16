/*
 * Copyright 2014 The Embulk project
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.embulk.util.file.InputStreamTransactionalFileInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalFileInputPlugin implements FileInputPlugin {
    public interface PluginTask extends Task {
        @Config("path_prefix")
        String getPathPrefix();

        @Config("last_path")
        @ConfigDefault("null")
        Optional<String> getLastPath();

        @Config("follow_symlinks")
        @ConfigDefault("false")
        boolean getFollowSymlinks();

        List<String> getFiles();

        void setFiles(List<String> files);
    }

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public ConfigDiff transaction(final ConfigSource config, final FileInputPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, PluginTask.class);

        // list files recursively
        final List<String> files = listFiles(task);
        logger.info("Loading files {}", files);
        task.setFiles(files);

        // number of processors is same with number of files
        final int taskCount = task.getFiles().size();
        return resume(task.dump(), taskCount, control);
    }

    @Override
    public ConfigDiff resume(final TaskSource taskSource, final int taskCount, final FileInputPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);

        control.run(taskSource, taskCount);

        // build next config
        final ConfigDiff configDiff = CONFIG_MAPPER_FACTORY.newConfigDiff();

        // last_path
        if (task.getFiles().isEmpty()) {
            // keep the last value
            if (task.getLastPath().isPresent()) {
                configDiff.set("last_path", task.getLastPath().get());
            }
        } else {
            final List<String> files = new ArrayList<String>(task.getFiles());
            Collections.sort(files);
            configDiff.set("last_path", files.get(files.size() - 1));
        }

        return configDiff;
    }

    @Override
    public void cleanup(final TaskSource taskSource, final int taskCount, final List<TaskReport> successTaskReports) {
    }

    @Override
    public TransactionalFileInput open(final TaskSource taskSource, final int taskIndex) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);

        final File file = new File(task.getFiles().get(taskIndex));

        return new InputStreamTransactionalFileInput(
                Exec.getBufferAllocator(),
                new InputStreamTransactionalFileInput.Opener() {
                    public InputStream open() throws IOException {
                        return new FileInputStream(file);
                    }
                }) {
            @Override
            public void abort() {}

            @Override
            public TaskReport commit() {
                return CONFIG_MAPPER_FACTORY.newTaskReport();
            }

            @Override
            public Optional<String> hintOfCurrentInputFileNameForLogging() {
                return Optional.ofNullable(file.getAbsolutePath());
            }
        };
    }

    static List<String> listFilesForTesting(final PluginTask task) {
        return listFiles(task);
    }

    private static List<String> listFiles(final PluginTask task) {
        // This |pathPrefixResolved| can still be a relative path from the working directory.
        // The path should not be normalized by Path#normalize to eliminate redundant name elements like "." and "..".
        final Path pathPrefixResolved = WORKING_DIRECTORY.resolve(Paths.get(task.getPathPrefix()));

        final Path dirToMatch;  // Directory part of "path_prefix" with character cases as specified.
        final Path dirToStartWalking;  //  Directory part of "path_prefix" with character cases as the real file system.
        final String baseFileNamePrefix;
        if (Files.isDirectory(pathPrefixResolved)) {
            // If |pathPrefixResolved| is actually an existing directory in the real file system.

            // Found paths are matched with the specified directory.
            dirToMatch = pathPrefixResolved;
            // Walking the tree starts from the specified directory.
            dirToStartWalking = getRealCasePathOfDirectoryNoFollowLinks(pathPrefixResolved);

            // Matching with any file ("*") in the directory.
            baseFileNamePrefix = "";
        } else {
            // If |pathPrefixResolved| is NOT a directory in the real file system.
            // A file that is exactly |pathPrefixResolved| may not exist, not just "NOT a directory".
            // Files and directories whose prefixes match |pathPrefixResolved| are expected.

            // Found paths are matched with the directory which contains the specified path.
            dirToMatch = Optional.ofNullable(pathPrefixResolved.getParent()).orElse(WORKING_DIRECTORY);
            // Walking the tree starts from the directory which contains the specified path.
            dirToStartWalking = (dirToMatch == WORKING_DIRECTORY
                                         ? WORKING_DIRECTORY
                                         : getRealCasePathOfDirectoryNoFollowLinks(dirToMatch));

            // Matching "{baseFileNamePrefix}*".
            baseFileNamePrefix = pathPrefixResolved.getFileName().toString();
        }
        final PathMatcher baseFileNameMatcher = buildPathMatcherForBaseFileNamePrefix(baseFileNamePrefix);
        final PathMatcher dirNameMatcher = buildPathMatcherForDirectory(dirToMatch);

        final ArrayList<String> filesFound = new ArrayList<>();
        final String lastPath = task.getLastPath().orElse(null);
        try {
            logger.info("Listing local files at directory '{}' filtering filename by prefix '{}'",
                        dirToMatch.equals(WORKING_DIRECTORY) ? "." : dirToMatch.toString(),
                        baseFileNamePrefix);

            final Set<FileVisitOption> visitOptions;
            if (task.getFollowSymlinks()) {
                visitOptions = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
            } else {
                visitOptions = EnumSet.noneOf(FileVisitOption.class);
                logger.info("\"follow_symlinks\" is set false. Note that symbolic links to directories are skipped.");
            }

            // |SimpleFileVisitor| visits files under a directory which matches |dirToStartWalking| in the following manners.
            //
            // * Linux: Case sensitive. It does not walk from "/FOO" when |dirToStartWalking| == "/foo".
            // * MacOSX: Case insensitive. It walks from "/FOO" when |dirToStartWalking| == "/foo".
            // * Windows: Case insensitive. It walks from "/FOO" when |dirToStartWalking| == "/foo".
            Files.walkFileTree(dirToStartWalking, visitOptions, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(final Path dirOnVisit, final BasicFileAttributes attrs) {
                        // NOTE: This |dirOnVisit| contains the path elements of |dirToStartWalking|.
                        if (dirOnVisit.equals(dirToStartWalking)) {
                            return FileVisitResult.CONTINUE;
                        } else if (lastPath != null && dirOnVisit.toString().compareTo(lastPath) <= 0) {
                            // TODO(dmikurube): Consider |Path#compareTo| instead of |String#compareTo|.
                            return FileVisitResult.SKIP_SUBTREE;
                        } else if (!dirNameMatcher.matches(dirOnVisit)) {
                            // |PathMatcher| (|dirNameMatcher|) matches paths in the following manners.
                            //
                            // * Linux: Case sensitive. It does not match "/FOO" when |dirNameMatcher| is with "/foo".
                            // * MacOSX: Case sensitive. It does not match "/FOO" when |dirNameMatcher| is with "/foo".
                            // * Windows: Case insensitive. It matches "/FOO" when |dirNameMatcher| is with "/foo".
                            //
                            // The case-sensitivity is different between |SimpleFileVisitor| and |PathMatcher| on OSX.
                            //
                            // To be consistent on OSX, it rejects case-unmatching paths by case-sensitive |PathMatcher|
                            // against paths visited by walking the tree with case-insensitive |SimpleFileVisitor|.
                            // It does not affect Linux (both are case-sensitive) nor Windows (both are case-insensitive).
                            return FileVisitResult.SKIP_SUBTREE;
                        } else {
                            final Path parent = Optional.ofNullable(dirOnVisit.getParent()).orElse(WORKING_DIRECTORY);
                            if (parent.equals(dirToStartWalking)) {
                                if (baseFileNameMatcher.matches(dirOnVisit.getFileName())) {
                                    return FileVisitResult.CONTINUE;
                                } else {
                                    return FileVisitResult.SKIP_SUBTREE;
                                }
                            } else {
                                return FileVisitResult.CONTINUE;
                            }
                        }
                    }

                    @Override
                    public FileVisitResult visitFile(final Path fileOnVisit, final BasicFileAttributes attrs) {
                        // NOTE: This |fileOnVisit| contains the path elements of |dirToStartWalking|.
                        try {
                            // Avoid directories from listing.
                            // Directories are normally unvisited with |FileVisitor#visitFile|, but symbolic links to
                            // directories are visited like files unless |FOLLOW_LINKS| is set in |Files#walkFileTree|.
                            // Symbolic links to directories are explicitly skipped here by checking with |Path#toReadlPath|.
                            if (Files.isDirectory(fileOnVisit.toRealPath())) {
                                return FileVisitResult.CONTINUE;
                            }
                        } catch (IOException ex) {
                            throw new RuntimeException("Can't resolve symbolic link", ex);
                        }
                        if (lastPath != null && fileOnVisit.toString().compareTo(lastPath) <= 0) {
                            // TODO(dmikurube): Consider |Path#compareTo| instead of |String#compareTo|.
                            return FileVisitResult.CONTINUE;
                        } else if (!dirNameMatcher.matches(fileOnVisit)) {
                            // Rejecting case-unmatching paths on OSX in the same way with |preVisitDirectory| above.
                            return FileVisitResult.CONTINUE;
                        } else {
                            final Path parent = Optional.ofNullable(fileOnVisit.getParent()).orElse(WORKING_DIRECTORY);
                            if (parent.equals(dirToStartWalking)) {
                                if (baseFileNameMatcher.matches(fileOnVisit.getFileName())) {
                                    filesFound.add(fileOnVisit.toString());
                                    return FileVisitResult.CONTINUE;
                                }
                            } else {
                                filesFound.add(fileOnVisit.toString());
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    }
                });
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Failed get a list of local files at '%s'", dirToMatch), ex);
        }
        return Collections.unmodifiableList(filesFound);
    }

    private static StringBuilder buildGlobPatternStringBuilder(final String pathString) {
        final StringBuilder globPatternBuilder = new StringBuilder();
        // Escape the special characters for the FileSystem#getPathMatcher().
        // See https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-
        pathString.chars().forEach(c -> {
            switch ((char) c) {
                case '*':
                case '?':
                case '{':
                case '}':
                case '[':
                case ']':
                case '\\':
                    globPatternBuilder.append('\\');
                    break;
                default:
                    break;
            }
            globPatternBuilder.append((char) c);
        });
        return globPatternBuilder;
    }

    private static PathMatcher buildPathMatcherForDirectory(final Path dir) {
        final String dirString = dir.toString();
        final int dirLength = dirString.length();
        final StringBuilder builder = buildGlobPatternStringBuilder(dirString);

        // |dir| can be empty when just a file/directory name directly on the working directory is given. For example,
        //
        //   path_prefix: file.txt
        if (dirLength > 1 && builder.charAt(dirLength - 1) != File.separatorChar) {
            if (File.separatorChar == '\\') {
                builder.append('\\');
            }
            builder.append(File.separator);
        }
        return FileSystems.getDefault().getPathMatcher("glob:" + builder.toString() + "**");
    }

    private static PathMatcher buildPathMatcherForBaseFileNamePrefix(final String baseFileNamePrefix) {
        final StringBuilder builder = buildGlobPatternStringBuilder(baseFileNamePrefix);
        return FileSystems.getDefault().getPathMatcher("glob:" + builder.toString() + "*");
    }

    /**
     * Returns a case-sensitive real path of a directory without resolving symbolic links.
     *
     * <p>{@code Path.toRealPath} looks to work to get a case-sensitive real path, but it may unintentionally resolve
     * symbolic links when resolving a case-sensitivity difference. To keep the option "follow_symlinks" working as
     * intended, a method to resolve cases without resolving symbolic links is required.
     */
    private static Path getRealCasePathOfDirectoryNoFollowLinks(final Path dirNormalized) {
        Path built;
        if (dirNormalized.isAbsolute()) {
            built = dirNormalized.getRoot();
        } else {
            built = Paths.get("");
        }
        for (final Path pathElement : dirNormalized) {
            if (pathElement.equals(DOT_DOT)) {
                built = built.resolve(DOT_DOT);
                continue;
            } else if (pathElement.equals(DOT)) {
                built = built.resolve(DOT);
                continue;
            }

            final Path startPath = built;
            final String pathElementString = pathElement.toString();
            final ArrayList<Path> found = new ArrayList<>();

            // FOLLOW_LINKS is intentionally set here. Imagine files on the file system are as below:
            //
            //   /Foo@ ( -> symbolic link to /bar)
            //   /bar/
            //   /bar/file1.txt
            //   /bar/dir/
            //   /bar/dir/file2.txt
            //
            // For this case, getRealCasePathOfDirectoryNoFollowLinks("/foo/dir") is expected to return "/Foo/dir",
            // neither "/foo/dir" nor /bar/dir". If FOLLOW_LINKS is not set here, "/Foo" is skipped unfortunately.
            //
            try {
                Files.walkFileTree(built, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 2,  new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                            if (dir.equals(startPath)) {
                                return FileVisitResult.CONTINUE;
                            }
                            final Path lastElement = dir.getFileName();
                            if (lastElement == null) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            if (pathElementString.equalsIgnoreCase(lastElement.toString())) {
                                found.add(dir);
                            }
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(final Path file, final IOException ex) throws IOException {
                            if (pathElementString.equalsIgnoreCase(file.getFileName().toString())) {
                                // It actually fails only when the failed file/directory is in interest.
                                throw ex;
                            } else {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        }
                    });
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }

            if (found.size() == 1) {
                built = found.get(0);
            } else if (found.size() > 1) {
                // If multiple paths are found, take the original. It means that the file system is case sensitive.
                built = built.resolve(pathElement);
            } else {
                throw new UncheckedIOException(new FileNotFoundException(
                        "Directory not found: \"" + built.resolve(pathElement).toString() + "\" for \""
                        + dirNormalized.toString() + "\""));
            }
        }
        return built;
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();

    // Java expects the working directory does not change during an execution.
    // @see <a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4045688">Bug ID: JDK-4045688 Add chdir or equivalent notion of changing working directory</a>
    private static final Path WORKING_DIRECTORY = Paths.get("").normalize();

    // http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap04.html
    // "The special filename dot shall refer to the directory specified by its predecessor.
    //  The special filename dot-dot shall refer to the parent directory of its predecessor directory.
    //  As a special case, in the root directory, dot-dot may refer to the root directory itself."
    private static final Path DOT = Paths.get(".");
    private static final Path DOT_DOT = Paths.get("..");

    private static final Logger logger = LoggerFactory.getLogger(LocalFileInputPlugin.class);
}
