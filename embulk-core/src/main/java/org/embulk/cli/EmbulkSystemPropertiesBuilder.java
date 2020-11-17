package org.embulk.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.embulk.EmbulkSystemProperties;
import org.slf4j.Logger;

/**
 * Builds the eventual Embulk system properties from the command line, environment variables, and the actual file system.
 */
class EmbulkSystemPropertiesBuilder {
    private EmbulkSystemPropertiesBuilder(
            final Properties commandLineProperties,
            final Map<String, String> env,
            final PathOrException userHomeOrEx,
            final PathOrException userDirOrEx,
            final Logger logger) {
        this.commandLineProperties = commandLineProperties;
        this.env = env;

        this.userHomeOrEx = userHomeOrEx;
        this.userDirOrEx = userDirOrEx;

        this.logger = logger;
    }

    static EmbulkSystemPropertiesBuilder from(
            final Properties javaSystemProperties,
            final Properties commandLineProperties,
            final Map<String, String> env,
            final Logger logger) {
        return new EmbulkSystemPropertiesBuilder(
                commandLineProperties,
                env,
                getUserHome(javaSystemProperties, logger),
                getUserDir(javaSystemProperties, logger),
                logger);
    }

    EmbulkSystemProperties buildProperties() {
        final Path embulkHome = findEmbulkHome();
        final Properties embulkPropertiesFromFile = loadEmbulkPropertiesFromFile(embulkHome);

        final Path m2Repo = findM2Repo(embulkHome, embulkPropertiesFromFile);
        final Path gemHome = findGemHome(embulkHome, embulkPropertiesFromFile);
        final List<Path> gemPath = findGemPath(embulkHome, embulkPropertiesFromFile);

        final Properties mergedProperties = new Properties();

        // 1) Properties from the "embulk.properties" file are loaded first.
        //
        // Later sources of properties would overwrite it.
        for (final String key : embulkPropertiesFromFile.stringPropertyNames()) {
            mergedProperties.setProperty(key, embulkPropertiesFromFile.getProperty(key));
        }

        // 2) Properties from the command-line are loaded second.
        //
        // It overwrites the properties from "embulk.properties".
        for (final String key : this.commandLineProperties.stringPropertyNames()) {
            mergedProperties.setProperty(key, this.commandLineProperties.getProperty(key));
        }

        // 3) Some specific properties are forcibly overwritten from file/directory lookups.
        mergedProperties.setProperty("embulk_home", embulkHome.toString());
        mergedProperties.setProperty("m2_repo", m2Repo.toString());
        mergedProperties.setProperty("gem_home", gemHome.toString());
        mergedProperties.setProperty(
                "gem_path", gemPath.stream().map(path -> path.toString()).collect(Collectors.joining(File.pathSeparator)));

        return EmbulkSystemProperties.of(mergedProperties);
    }

    private static PathOrException getUserHome(final Properties javaSystemProperties, final Logger logger) {
        return normalizePathInJavaSystemProperty("user.home", javaSystemProperties, logger);
    }

    private static PathOrException getUserDir(final Properties javaSystemProperties, final Logger logger) {
        return normalizePathInJavaSystemProperty("user.dir", javaSystemProperties, logger);
    }

    /**
     * Finds an appropriate "embulk_home" directory based on a rule defined.
     *
     * <ul>
     * <li>1) If a system config {@code "embulk_home"} is set from the command line, it is the most prioritized.
     * <li>2) If an environment variable {@code "EMBULK_HOME"} is set, it is the second prioritized.
     * <li>3) If neither (1) nor (2) is set, it iterates up over parent directories from "user.dir" for a directory that:
     *   <ul>
     *   <li>is named ".embulk",
     *   <li>has "embulk.properties" just under itself.
     *   </ul>
     *   <ul>
     *   <li>3-1) If "user.dir" (almost equal to the working directory) is under "user.home", it iterates up till "user.home".
     *   <li>3-2) If "user.dir" is not under "user.home", Embulk iterates until the root directory.
     *   </ul>
     * </li>
     * <li>4) If none of the above does not work, use the traditional predefined directory "~/.embulk".
     * </ul>
     */
    private Path findEmbulkHome() {
        // 1) If a system config "embulk_home" is set from the command line, it is the most prioritized.
        final Optional<Path> ofCommandLine = normalizePathInCommandLineProperties("embulk_home");
        if (ofCommandLine.isPresent()) {
            logger.info("embulk_home is set from command-line: {}", ofCommandLine.get().toString());
            return ofCommandLine.get();
        }

        // 2) If an environment variable "EMBULK_HOME" is set, it is the second prioritized.
        final Optional<Path> ofEnv = normalizePathInEnv("EMBULK_HOME");
        if (ofEnv.isPresent()) {
            logger.info("embulk_home is set from environment variable: {}", ofEnv.get().toString());
            return ofEnv.get();
        }

        // (3) and (4) depend on "user.home" and "user.dir". Exception if they are unavailable.
        final Path userHome = userHomeOrEx.orRethrow();
        final Path userDir = userDirOrEx.orRethrow();

        final Path iterateUpTill;
        if (isUnderHome()) {
            // 3-1) If "user.dir" (almost equal to the working directory) is under "user.home", it iterates up till "user.home".
            iterateUpTill = userHome;
        } else {
            // 3-2) If "user.dir" is not under "user.home", it iterates up till the root directory.
            iterateUpTill = userDir.getRoot();
        }

        // 3) If neither (1) nor (2) is set, it iterates up over parent directories from "user.dir" for a directory that:
        //   * is named ".embulk",
        //   * has a readable file "embulk.properties" just under itself.
        if (iterateUpTill != null) {
            for (Path pwd = userDir; pwd != null && pwd.startsWith(iterateUpTill); pwd = pwd.getParent()) {
                // When checking the actual file/directory, symbolic links are resolved.

                final Path dotEmbulk;
                try {
                    dotEmbulk = pwd.resolve(".embulk");
                    if (Files.notExists(dotEmbulk) || (!Files.isDirectory(dotEmbulk))) {
                        continue;
                    }
                } catch (final RuntimeException ex) {
                    logger.debug("Failed to check for \".embulk\" at: " + pwd.toString(), ex);
                    continue;
                }

                try {
                    final Path properties = dotEmbulk.resolve("embulk.properties");
                    if (Files.notExists(properties) || (!Files.isRegularFile(properties)) || (!Files.isReadable(properties))) {
                        continue;
                    }
                } catch (final RuntimeException ex) {
                    logger.debug("Failed to check for \"embulk.properties\" at: " + dotEmbulk.toString(), ex);
                    continue;
                }

                logger.info("embulk_home is set by the location of embulk.properties found in: {}", dotEmbulk.toString());
                return dotEmbulk;
            }
        }

        // 4) If none of the above does not work, use the traditional predefined directory "~/.embulk".
        return userHome.resolve(".embulk");
    }

    private Properties loadEmbulkPropertiesFromFile(final Path embulkHome) {
        final Path path = embulkHome.resolve("embulk.properties");

        if (Files.notExists(path)) {
            logger.debug(path.toString() + " does not exist. Ignored.");
            return new Properties();
        }
        if (!Files.isRegularFile(path)) {
            logger.info(path.toString() + " exists, but not a regular file. Ignored.");
            return new Properties();
        }
        if (!Files.isReadable(path)) {
            logger.info(path.toString() + " exists, but not readable. Ignored.");
            return new Properties();
        }

        final Properties properties = new Properties();
        try (final InputStream input = Files.newInputStream(path, StandardOpenOption.READ)) {
            properties.load(input);
        } catch (final IOException ex) {
            logger.warn(path.toString() + " exists, but failed to load. Ignored.", ex);
            return new Properties();
        }
        return properties;
    }

    private Path findM2Repo(final Path embulkHome, final Properties embulkPropertiesFromFile) {
        return findSubdirectory(embulkHome, embulkPropertiesFromFile, "m2_repo", "M2_REPO", M2_REPO_RELATIVE);
    }

    private Path findGemHome(final Path embulkHome, final Properties embulkPropertiesFromFile) {
        return findSubdirectory(embulkHome, embulkPropertiesFromFile, "gem_home", "GEM_HOME", GEM_HOME_RELATIVE);
    }

    private List<Path> findGemPath(final Path embulkHome, final Properties embulkPropertiesFromFile) {
        return findSubdirectories(embulkHome, embulkPropertiesFromFile, "gem_path", "GEM_PATH");
    }

    private Path findSubdirectory(
            final Path embulkHome,
            final Properties embulkPropertiesFromFile,
            final String propertyName,
            final String envName,
            final Path subPath) {
        // 1) If a system config <propertyName> is set from the command line, it is the most prioritized.
        //
        // A path in the command line should be an absolute path, or a relative path from the working directory.
        final Optional<Path> ofCommandLine = normalizePathInCommandLineProperties(propertyName);
        if (ofCommandLine.isPresent()) {
            return ofCommandLine.get();
        }

        // 2) If a system config <propertyName> is set from "embulk.properties", it is the second prioritized.
        //
        // A path in the "embulk.properties" file should be an absolute path, or a relative path from "embulk_home".
        final Optional<Path> ofEmbulkPropertiesFile =
                normalizePathInEmbulkPropertiesFile(propertyName, embulkPropertiesFromFile, embulkHome);
        if (ofEmbulkPropertiesFile.isPresent()) {
            return ofEmbulkPropertiesFile.get();
        }

        // 3) If an environment variable <envName> is set, it is the third prioritized.
        //
        // A path in an environment variable should be an absolute path.
        final Optional<Path> ofEnv = normalizePathInEnv(envName);
        if (ofEnv.isPresent()) {
            return ofEnv.get();
        }

        // 4) If none of the above does not match, use the specific sub directory of "embulk_home".
        return embulkHome.resolve(subPath);
    }

    private List<Path> findSubdirectories(
            final Path embulkHome,
            final Properties embulkPropertiesFromFile,
            final String propertyName,
            final String envName) {
        // 1) If a system config <propertyName> is set from the command line, it is the most prioritized.
        final List<Path> ofCommandLine = normalizePathsInCommandLineProperties(propertyName, true);
        if (!ofCommandLine.isEmpty()) {
            return ofCommandLine;
        }

        // 2) If a system config <propertyName> is set from "embulk.properties", it is the second prioritized.
        final List<Path> ofEmbulkPropertiesFile =
                normalizePathsInEmbulkPropertiesFile(propertyName, embulkPropertiesFromFile, embulkHome, true);
        if (!ofEmbulkPropertiesFile.isEmpty()) {
            return ofEmbulkPropertiesFile;
        }

        // 3) If an environment variable <envName> is set, it is the third prioritized.
        final List<Path> ofEnv = normalizePathsInEnv(envName, true);
        if (!ofEnv.isEmpty()) {
            return ofEnv;
        }

        // 4) If none of the above does not match, return an empty list.
        return Collections.unmodifiableList(new ArrayList<>());
    }

    /**
     * Returns a normalized path in a specified Java system property "user.home" or "user.dir".
     *
     * <p>Note that a path in a Java system property should be an absolute path.
     */
    private static PathOrException normalizePathInJavaSystemProperty(
            final String propertyName, final Properties javaSystemProperties, final Logger logger) {
        final String property = javaSystemProperties.getProperty(propertyName);

        if (property == null || property.isEmpty()) {
            final String message = "Java system property \"" + propertyName + "\" is unexpectedly unset.";
            final IllegalArgumentException ex = new IllegalArgumentException(message);
            logger.error(message, ex);
            return new PathOrException(ex);
        }

        final Path path;
        try {
            path = Paths.get(property);
        } catch (final InvalidPathException ex) {
            logger.error("Java system property \"" + propertyName + "\" is unexpectedly invalid: \"" + property + "\"", ex);
            return new PathOrException(ex);
        }

        if (!path.isAbsolute()) {
            final String message = "Java system property \"" + propertyName + "\" is unexpectedly not absolute.";
            final IllegalArgumentException ex = new IllegalArgumentException(message);
            logger.error(message, ex);
            return new PathOrException(ex);
        }

        final Path normalized = path.normalize();
        if (!normalized.equals(path)) {
            logger.warn("Java system property \"" + propertyName + "\" is unexpectedly not normalized: \"" + property + "\", "
                                + "then resolved to: \"" + normalized.toString() + "\"");
        }

        // Symbolic links are intentionally NOT resolved with Path#toRealPath.
        return new PathOrException(normalized);
    }

    private Optional<Path> normalizePathInCommandLineProperties(final String propertyName) {
        final List<Path> paths = normalizePathsInCommandLineProperties(propertyName, false);
        if (paths.size() > 1) {
            throw new IllegalStateException("Multiple paths returned for an unsplit path.");
        }

        if (paths.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(paths.get(0));
    }

    /**
     * Returns normalized paths in a specified property from the command line.
     *
     * <p>Note that a path in the command line should be an absolute path, or a relative path from the working directory.
     */
    private List<Path> normalizePathsInCommandLineProperties(final String propertyName, final boolean multi) {
        if (!this.commandLineProperties.containsKey(propertyName)) {
            return Collections.unmodifiableList(new ArrayList<Path>());
        }
        final String property = this.commandLineProperties.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<Path>());
        }

        final List<String> pathStrings = splitPathStrings(property, multi);
        final ArrayList<Path> paths = new ArrayList<>();
        for (final String pathString : pathStrings) {
            if (pathString.isEmpty()) {
                continue;
            }

            final Path path;
            try {
                path = Paths.get(pathString);
            } catch (final InvalidPathException ex) {
                logger.error("Embulk system property \"" + propertyName + "\" in command-line is invalid: \"" + pathString + "\"", ex);
                throw ex;
            }

            final Path absolute;
            if (path.isAbsolute()) {
                absolute = path;
            } else {
                absolute = path.toAbsolutePath();
            }

            final Path normalized = absolute.normalize();
            if (!normalized.equals(path)) {
                logger.warn("Embulk system property \"" + propertyName + "\" in command-line is not normalized: "
                             + "\"" + pathString + "\", " + "then resolved to: \"" + normalized.toString() + "\"");
            }

            // Symbolic links are intentionally NOT resolved with Path#toRealPath.
            paths.add(normalized);
        }

        return Collections.unmodifiableList(paths);
    }

    private Optional<Path> normalizePathInEnv(final String envName) {
        final List<Path> paths = normalizePathsInEnv(envName, false);
        if (paths.size() > 1) {
            throw new IllegalStateException("Multiple paths returned for an unsplit path.");
        }

        if (paths.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(paths.get(0));
    }

    /**
     * Returns normalized paths in an environment variable.
     *
     * <p>Note that a path in an environment variable should be an absolute path.
     */
    private List<Path> normalizePathsInEnv(final String envName, final boolean multi) {
        if (!this.env.containsKey(envName)) {
            return Collections.unmodifiableList(new ArrayList<Path>());
        }
        final String value = this.env.get(envName);
        if (value == null || value.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<Path>());
        }

        final List<String> pathStrings = splitPathStrings(value, multi);
        final ArrayList<Path> paths = new ArrayList<>();
        for (final String pathString : pathStrings) {
            if (pathString.isEmpty()) {
                continue;
            }

            final Path path;
            try {
                path = Paths.get(pathString);
            } catch (final InvalidPathException ex) {
                logger.error("Environment variable \"" + envName + "\" is invalid: \"" + pathString + "\"", ex);
                throw ex;
            }

            if (!path.isAbsolute()) {
                final String message = "Environment variable \"" + envName + "\" is not absolute.";
                final IllegalArgumentException ex = new IllegalArgumentException(message);
                logger.error(message, ex);
                throw ex;
            }

            final Path normalized = path.normalize();
            if (!normalized.equals(path)) {
                logger.warn("Environment variable \"" + envName + "\" is not normalized: "
                             + "\"" + pathString + "\", " + "then resolved to: \"" + normalized.toString() + "\"");
            }

            // Symbolic links are intentionally NOT resolved with Path#toRealPath.
            paths.add(normalized);
        }

        return Collections.unmodifiableList(paths);
    }

    private Optional<Path> normalizePathInEmbulkPropertiesFile(
            final String propertyName,
            final Properties embulkPropertiesFromFile,
            final Path embulkHome) {
        final List<Path> paths = normalizePathsInEmbulkPropertiesFile(
                propertyName, embulkPropertiesFromFile, embulkHome, false);
        if (paths.size() > 1) {
            throw new IllegalStateException("Multiple paths returned for an unsplit path.");
        }

        if (paths.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(paths.get(0));
    }

    /**
     * Returns normalized paths from the "embulk.properties" file.
     *
     * <p>Note that a path in the "embulk.properties" file should be an absolute path, or a relative path from "embulk_home".
     */
    private List<Path> normalizePathsInEmbulkPropertiesFile(
            final String propertyName,
            final Properties embulkPropertiesFromFile,
            final Path embulkHome,
            final boolean multi) {
        if (!embulkPropertiesFromFile.containsKey(propertyName)) {
            return Collections.unmodifiableList(new ArrayList<Path>());
        }
        final String property = embulkPropertiesFromFile.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<Path>());
        }

        final List<String> pathStrings = splitPathStrings(property, multi);
        final ArrayList<Path> paths = new ArrayList<>();
        for (final String pathString : pathStrings) {
            if (pathString.isEmpty()) {
                continue;
            }

            final Path path;
            try {
                path = Paths.get(pathString);
            } catch (final InvalidPathException ex) {
                logger.error(
                        "Embulk system property \"" + propertyName + "\" in embulk.properties is invalid: \"" + pathString + "\"",
                        ex);
                throw ex;
            }

            final Path absolute;
            if (path.isAbsolute()) {
                absolute = path;
            } else {
                absolute = embulkHome.resolve(path);
            }

            final Path normalized = absolute.normalize();
            if (!normalized.equals(path)) {
                logger.warn("Embulk system property \"" + propertyName + "\" in embulk.properties is not normalized: "
                             + "\"" + pathString + "\", " + "then resolved to: \"" + normalized.toString() + "\"");
            }

            // Symbolic links are intentionally NOT resolved with Path#toRealPath.
            paths.add(normalized);
        }

        return Collections.unmodifiableList(paths);
    }

    private List<String> splitPathStrings(final String pathStrings, final boolean multi) {
        final ArrayList<String> split = new ArrayList<>();
        if (multi) {
            for (final String pathString : pathStrings.split(File.pathSeparator)) {
                split.add(pathString);
            }
        } else {
            split.add(pathStrings);
        }
        return Collections.unmodifiableList(split);
    }

    /**
     * Returns {@code true} if {@code userDir} is under {@code userHome}.
     *
     * <p>Note that the check is performed "literally". It does not take care of the existence of the path.
     * It does not resolve a symbolic link.
     */
    private boolean isUnderHome() {
        return this.userDirOrEx.orRethrow().startsWith(this.userHomeOrEx.orRethrow());
    }

    /**
     * Contains a Path, or an Exception in case the Path is invalid.
     *
     * <p>It is used for Java system properties "user.home" and "user.dir" to delay throwing the Exception.
     *
     * <p>Even if "user.home" or "user.dir" is invalid, it should be okay when "embulk_home" is configured explicitly.
     */
    private static class PathOrException {
        PathOrException(final Path path) {
            if (path == null) {
                this.path = null;
                this.exception = new NullPointerException("Path is null.");
            } else {
                this.path = path;
                this.exception = null;
            }
        }

        PathOrException(final RuntimeException exception) {
            this.path = null;
            this.exception = exception;
        }

        Path orRethrow() {
            if (this.path == null) {
                throw this.exception;
            }
            return this.path;
        }

        private final Path path;
        private final RuntimeException exception;
    }

    private static final Path M2_REPO_RELATIVE = Paths.get("lib").resolve("m2").resolve("repository");
    private static final Path GEM_HOME_RELATIVE = Paths.get("lib").resolve("gems");

    private final Properties commandLineProperties;
    private final Map<String, String> env;

    private final PathOrException userHomeOrEx;
    private final PathOrException userDirOrEx;

    private final Logger logger;
}
