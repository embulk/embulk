package org.embulk.cli;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.embulk.jruby.ScriptingContainerDelegateImpl;

public class EmbulkMigrate {
    public void migratePlugin(final String pathInString, final String thisEmbulkVersion) throws IOException {
        migratePlugin(Paths.get(pathInString), thisEmbulkVersion);
    }

    public void migratePlugin(final Path path, final String thisEmbulkVersion) throws IOException {
        final Migrator migrator = new Migrator(path);

        List<Matcher> matchedEmbulkCoreInGradle = migrator.matchersRecursive("build.gradle", EMBULK_CORE_IN_GRADLE);
        List<Matcher> matchedNewEmbulkInGemspec = migrator.matchersRecursive("*.gemspec", NEW_EMBULK_IN_GEMSPEC);
        List<Matcher> matchedOldEmbulkInGemspec = migrator.matchersRecursive("*.gemspec", OLD_EMBULK_IN_GEMSPEC);

        final Language language;
        final ComparableVersion fromVersion;
        if (!matchedEmbulkCoreInGradle.isEmpty()) {
            language = Language.JAVA;
            fromVersion = new ComparableVersion(matchedEmbulkCoreInGradle.get(0).group(1).replace("+", "0"));
            System.out.printf("Detected Java plugin for Embulk %s...\n", fromVersion.toString());
        } else if (!matchedNewEmbulkInGemspec.isEmpty()) {
            language = Language.RUBY;
            fromVersion = new ComparableVersion(matchedNewEmbulkInGemspec.get(0).group(1));
            System.out.printf("Detected Ruby plugin for Embulk %s...\n", fromVersion.toString());
        } else if (!matchedOldEmbulkInGemspec.isEmpty()) {
            language = Language.RUBY;
            fromVersion = new ComparableVersion("0.1.0");
            System.out.println("Detected Ruby plugin for unknown Embulk version...");
        } else {
            throw new RuntimeException("Failed to detect plugin language and dependency version");
        }

        switch (language) {
            case JAVA:
                migrateJavaPlugin(migrator, fromVersion, thisEmbulkVersion);
                break;
            case RUBY:
                migrateRubyPlugin(migrator, fromVersion, thisEmbulkVersion);
                break;
            default:  // Never default as all enums are listed.
        }

        if (migrator.getModifiedFiles().isEmpty()) {
            System.out.println("Done. No files are modified.");
        } else {
            System.out.println("Done. Please check modified files.");
        }
    }

    @SuppressWarnings("checkstyle:LineLength")
    private void migrateJavaPlugin(final Migrator migrator,
                                   final ComparableVersion fromVersion,
                                   final String thisEmbulkVersion) throws IOException {
        if (fromVersion.compareTo(new ComparableVersion("0.7.0")) < 0) {
            // rename CommitReport to TaskReport
            migrator.replaceRecursive("*.java", Pattern.compile("(CommitReport)"), 1, "TaskReport");
            migrator.replaceRecursive("*.java", Pattern.compile("(commitReport)"), 1, "taskReport");
        }

        // upgrade gradle version
        if (migrator.match("gradle/wrapper/gradle-wrapper.properties", GRADLE_VERSION_IN_WRAPPER)) {
            // gradle < 4.1
            migrator.copy("org/embulk/plugin/template/java/gradlew", "gradlew");
            migrator.setExecutable("gradlew");
            migrator.copy("org/embulk/plugin/template/java/gradle/wrapper/gradle-wrapper.properties",
                          "gradle/wrapper/gradle-wrapper.properties");
            migrator.copy("org/embulk/plugin/template/java/gradle/wrapper/gradle-wrapper.jar",
                          "gradle/wrapper/gradle-wrapper.jar");
        }

        // Add a method |jsonColumn| before the method |timestampColumn| which should exist.
        if (!migrator.matchRecursive("*.java", JSON_COLUMN_METHOD_IN_ALL_JAVA)) {
            final List<Matcher> matchers = migrator.matchersRecursive("*.java", TIMESTAMP_COLUMN_METHOD_IN_ALL_JAVA);
            if (!matchers.isEmpty()) {
                final String indent = matchers.get(0).group(1);
                final String jsonColumnMethod = Joiner.on("\n").join(
                        "",
                        indent + "public void jsonColumn(Column column) {",
                        indent + "    throw new UnsupportedOperationException(\"This plugin doesn't support json type. Please try to upgrade version of the plugin using 'embulk gem update' command. If the latest version still doesn't support json type, please contact plugin developers, or change configuration of input plugin not to use json type.\");",
                        indent + "}",
                        "",
                        indent + "@Override",
                        "");
                migrator.replaceRecursive(
                        "*.java",
                        TIMESTAMP_COLUMN_METHOD_AFTER_NEWLINE_IN_ALL_JAVA,
                        1,
                        jsonColumnMethod);
            }
        }

        // Add |sourceCompatibility| and |targetCompatibility| in build.gradle before |dependencies| existing.
        if (!migrator.match("build.gradle", TARGET_COMPATIBILITY_IN_GRADLE)) {
            migrator.insertLine("build.gradle", DEPENDENCIES_IN_GRADLE, new StringUpsert() {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        return String.format("%stargetCompatibility = 1.8\n", matcher.group(1));
                    }
                });
        }
        if (!migrator.match("build.gradle", SOURCE_COMPATIBILITY_IN_GRADLE)) {
            migrator.insertLine("build.gradle", TARGET_COMPATIBILITY_IN_GRADLE_WITH_INDENT, new StringUpsert() {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        return String.format("%ssourceCompatibility = 1.8\n", matcher.group(1));
                    }
                });
        }

        // Add the |checkstyle| Gradle plugin before the |java| plugin.
        if (!migrator.match("build.gradle", CHECKSTYLE_PLUGIN_IN_GRADLE)) {
            migrator.insertLine("build.gradle", JAVA_PLUGIN_IN_GRADLE, new StringUpsert() {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        return String.format("%sid%s%scheckstyle%s",
                                             matcher.group(1),
                                             matcher.group(2),
                                             matcher.group(3),
                                             matcher.group(3));
                    }
                });
            migrator.copy("org/embulk/plugin/template/java/config/checkstyle/checkstyle.xml",
                          "config/checkstyle/checkstyle.xml");
        }

        // Add |checkstyle| settings before the |gem| task existing.
        if (!migrator.match("build.gradle", CHECKSTYLE_CONFIGURATION_IN_GRADLE)) {
            migrator.copy("org/embulk/plugin/template/java/config/checkstyle/default.xml",
                          "config/checkstyle/default.xml");
            migrator.insertLine("build.gradle", GEM_TASK_IN_GRADLE, new StringUpsert() {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        final Joiner joiner = Joiner.on("\n");
                        final String indent = matcher.group(1);
                        return joiner.join(
                            indent + "checkstyle {",
                            indent + "    configFile = file(\"${project.rootDir}/config/checkstyle/checkstyle.xml\")",
                            indent + "    toolVersion = '6.14.1'",
                            indent + "}",
                            indent + "checkstyleMain {",
                            indent + "    configFile = file(\"${project.rootDir}/config/checkstyle/default.xml\")",
                            indent + "    ignoreFailures = true",
                            indent + "}",
                            indent + "checkstyleTest {",
                            indent + "    configFile = file(\"${project.rootDir}/config/checkstyle/default.xml\")",
                            indent + "    ignoreFailures = true",
                            indent + "}",
                            indent + "task checkstyle(type: Checkstyle) {",
                            indent + "    classpath = sourceSets.main.output + sourceSets.test.output",
                            indent + "    source = sourceSets.main.allJava + sourceSets.test.allJava",
                            indent + "}");
                    }
                });
        }

        // Update |embulk-core| and |embulk-standards| versions depending.
        migrator.replaceRecursive("build.gradle", EMBULK_CORE_OR_STANDARDS_IN_GRADLE, 1, thisEmbulkVersion);
    }

    private void migrateRubyPlugin(final Migrator migrator,
                                   final ComparableVersion fromVersion,
                                   final String thisEmbulkVersion) throws IOException {
        final String jrubyVersion = ScriptingContainerDelegateImpl.getJRubyVersion(EmbulkMigrate.class.getClassLoader());
        if (jrubyVersion != null) {
            migrator.write(".ruby-version", "jruby-" + jrubyVersion);
        } else {
            System.err.println("JRuby version not found. No .ruby-version is created nor migrated.");
        }

        // Update |embulk| version depending.
        if (fromVersion.compareTo(new ComparableVersion("0.1.0")) <= 0) {
            // Add add_development_dependency.
            migrator.insertLineRecursive("*.gemspec", DEVELOPMENT_DEPENDENCY_IN_GEMSPEC, new StringUpsert() {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        return String.format("%s.add_development_dependency 'embulk', ['>= %s']",
                                             matcher.group(1),
                                             thisEmbulkVersion);
                    }
                });
        } else {
            if (migrator.replaceRecursive("*.gemspec", EMBULK_DEPENDENCY_PRERELEASE_IN_GEMSPEC, 1,
                                 ">= " + thisEmbulkVersion).isEmpty()) {
                migrator.replaceRecursive("*.gemspec", EMBULK_DEPENDENCY_IN_GEMSPEC, 1, thisEmbulkVersion);
            }
        }
    }

    private class Migrator {
        private Migrator(Path basePath) {
            this.basePath = basePath;
            this.modifiedFiles = new HashSet<Path>();
        }

        public Path getBasePath() {
            return this.basePath;
        }

        public Set<Path> getModifiedFiles() {
            return this.modifiedFiles;
        }

        public void copy(String sourceResourcePath, String destinationFileName) throws IOException {
            Path destinationPath = this.basePath.resolve(destinationFileName);
            Files.createDirectories(destinationPath.getParent());
            Files.copy(EmbulkMigrate.class.getClassLoader().getResourceAsStream(sourceResourcePath), destinationPath,
                    StandardCopyOption.REPLACE_EXISTING);
            this.modifiedFiles.add(destinationPath);
        }

        public boolean match(String filePath, Pattern pattern) throws IOException {
            final Matcher matcher = pattern.matcher(read(this.basePath.resolve(filePath)));
            return matcher.find();
        }

        public boolean matchRecursive(String baseFileNameGlob, Pattern pattern) throws IOException {
            return !matchersRecursive(baseFileNameGlob, pattern).isEmpty();
        }

        public List<Matcher> matchersRecursive(final String baseFileNameGlob, final Pattern pattern)
                throws IOException {
            final ImmutableList.Builder<Matcher> matchers = ImmutableList.<Matcher>builder();
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + baseFileNameGlob);
            Files.walkFileTree(this.basePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attributes) throws IOException {
                        if (pathMatcher.matches(filePath.getFileName())) {
                            final Matcher matcher = pattern.matcher(read(filePath));
                            if (matcher.find()) {
                                matchers.add(matcher);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            return matchers.build();
        }

        public List<Matcher> replaceRecursive(final String baseFileNameGlob,
                                              final Pattern pattern,
                                              final int index,
                                              final String immediate) throws IOException {
            final ImmutableList.Builder<Matcher> matchers = ImmutableList.<Matcher>builder();
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + baseFileNameGlob);
            Files.walkFileTree(this.basePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attributes) throws IOException {
                        if (pathMatcher.matches(filePath.getFileName())) {
                            final String originalData = read(filePath);
                            Matcher first = null;
                            int position = 0;
                            String modifiedData = originalData;
                            while (position < modifiedData.length()) {
                                final String formerModifiedData = modifiedData.substring(0, position);
                                final String latterModifiedData = modifiedData.substring(position);
                                final Matcher matcher = pattern.matcher(latterModifiedData);
                                if (!matcher.find()) {
                                    break;
                                }
                                if (first == null) {
                                    first = matcher;
                                }
                                final String replacingString = immediate;
                                modifiedData =
                                        formerModifiedData
                                        + latterModifiedData.substring(0, matcher.start(index))
                                        + replacingString
                                        + latterModifiedData.substring(matcher.end(index));
                                position =
                                        formerModifiedData.length()
                                        + matcher.start(index)
                                        + replacingString.length()
                                        + (matcher.end() - matcher.end(index));
                            }
                            if (first != null) {
                                modify(filePath, modifiedData);
                                matchers.add(first);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            return matchers.build();
        }

        public boolean insertLine(Path filePath, Pattern pattern, StringUpsert stringUpsert)
                throws IOException {
            final String originalData = read(filePath);
            final Matcher matcher = pattern.matcher(originalData);
            if (matcher.find()) {
                final String preMatch = originalData.substring(0, matcher.start());
                final int lineNumber = preMatch.split("\n").length;
                final String replacingString = stringUpsert.getUpsertd(matcher);
                List<String> lines = new ArrayList<String>(Arrays.asList(originalData.split("\n")));
                lines.add(lineNumber + 1, replacingString);
                final String modifiedData = Joiner.on("\n").join(lines);
                modify(filePath, modifiedData);
                return true;
            }
            return false;
        }

        public boolean insertLine(String filePath, Pattern pattern, StringUpsert stringUpsert)
                throws IOException {
            return insertLine(this.basePath.resolve(filePath), pattern, stringUpsert);
        }

        public void insertLineRecursive(final String baseFileNameGlob,
                                        final Pattern pattern,
                                        final StringUpsert stringUpsert) throws IOException {
            final ImmutableList.Builder<Matcher> matchers = ImmutableList.<Matcher>builder();
            final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + baseFileNameGlob);
            Files.walkFileTree(this.basePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attributes) throws IOException {
                        if (pathMatcher.matches(filePath.getFileName())) {
                            insertLine(filePath, pattern, stringUpsert);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        }

        public void write(String fileName, String writtenData) throws IOException {
            Path destinationPath = this.basePath.resolve(fileName);
            Files.createDirectories(destinationPath.getParent());
            modify(destinationPath, writtenData);
        }

        private void modify(Path filePath, String modifiedData) throws IOException {
            final String originalData = read(filePath);
            if (!originalData.equals(modifiedData)) {
                Files.write(filePath, modifiedData.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                if (!this.modifiedFiles.contains(filePath)) {
                    if (originalData.isEmpty()) {
                        System.out.printf("  Created %s\n", filePath.toString());
                    } else {
                        System.out.printf("  Modified %s\n", filePath.toString());
                    }
                    this.modifiedFiles.add(filePath);
                }
            }
        }

        private String read(Path filePath) throws IOException {
            // assumes source code is written in UTF-8.
            return new String(readBytes(filePath), StandardCharsets.UTF_8);
        }

        private byte[] readBytes(Path filePath) throws IOException {
            try {
                return Files.readAllBytes(filePath);
            } catch (NoSuchFileException ex) {
                return new byte[0];
            }
        }

        private void setExecutable(String targetFileName) throws IOException {
            final Path targetPath = this.basePath.resolve(targetFileName);
            final Set<PosixFilePermission> permissions =
                    new HashSet<PosixFilePermission>(Files.getPosixFilePermissions(targetPath));
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(targetPath, permissions);
        }

        private final Path basePath;
        private final Set<Path> modifiedFiles;
    }

    private interface StringUpsert {
        public String getUpsertd(Matcher matcher);
    }

    private enum Language {
        JAVA,
        RUBY,
        ;
    }

    private static final Pattern EMBULK_CORE_IN_GRADLE = Pattern.compile(
            "org\\.embulk:embulk-core:([\\d\\.\\+]+)?");
    private static final Pattern NEW_EMBULK_IN_GEMSPEC = Pattern.compile(
            "add_(?:development_)?dependency\\s+\\W+embulk\\W+\\s+([\\d\\.]+)\\W+");
    private static final Pattern OLD_EMBULK_IN_GEMSPEC = Pattern.compile(
            "embulk-");
    private static final Pattern GRADLE_VERSION_IN_WRAPPER = Pattern.compile(
            "gradle-[23]\\.\\d+(\\.\\d+)?-");
    private static final Pattern JSON_COLUMN_METHOD_IN_ALL_JAVA = Pattern.compile(
            "void\\s+jsonColumn");
    private static final Pattern TIMESTAMP_COLUMN_METHOD_IN_ALL_JAVA = Pattern.compile(
            "^(\\W+).*?void\\s+timestampColumn", Pattern.MULTILINE);
    private static final Pattern TIMESTAMP_COLUMN_METHOD_AFTER_NEWLINE_IN_ALL_JAVA = Pattern.compile(
            "(\\r?\\n)(\\W+).*?void\\s+timestampColumn");
    private static final Pattern TARGET_COMPATIBILITY_IN_GRADLE = Pattern.compile(
            "targetCompatibility");
    private static final Pattern SOURCE_COMPATIBILITY_IN_GRADLE = Pattern.compile(
            "sourceCompatibility");
    private static final Pattern DEPENDENCIES_IN_GRADLE = Pattern.compile(
            "^([ \\t]*)dependencies\\s*\\{", Pattern.MULTILINE);
    private static final Pattern TARGET_COMPATIBILITY_IN_GRADLE_WITH_INDENT = Pattern.compile(
            "^([ \\t]*)targetCompatibility", Pattern.MULTILINE);
    private static final Pattern CHECKSTYLE_PLUGIN_IN_GRADLE = Pattern.compile(
            "id\\s+(?<quote>[\"\'])checkstyle\\k<quote>");
    private static final Pattern JAVA_PLUGIN_IN_GRADLE = Pattern.compile(
            "^([ \t]*)id( +)([\"\'])java[\"\']", Pattern.MULTILINE);
    private static final Pattern CHECKSTYLE_CONFIGURATION_IN_GRADLE = Pattern.compile(
            "checkstyle\\s+\\{");
    private static final Pattern GEM_TASK_IN_GRADLE = Pattern.compile(
            "^([ \\t]*)task\\s+gem\\W.*\\{", Pattern.MULTILINE);
    private static final Pattern EMBULK_CORE_OR_STANDARDS_IN_GRADLE = Pattern.compile(
            "org\\.embulk:embulk-(?:core|standards|test):([\\d\\.\\+]+)?");
    private static final Pattern DEVELOPMENT_DEPENDENCY_IN_GEMSPEC = Pattern.compile(
            "([ \\t]*\\w+)\\.add_development_dependency");
    private static final Pattern EMBULK_DEPENDENCY_PRERELEASE_IN_GEMSPEC = Pattern.compile(
            "add_(?:development_)?dependency\\s+\\W+embulk\\W+\\s*(\\~\\>\\s*[\\d\\.]+)\\W+");
    private static final Pattern EMBULK_DEPENDENCY_IN_GEMSPEC = Pattern.compile(
            "add_(?:development_)?dependency\\s+\\W+embulk\\W+\\s*([\\d\\.]+)\\W+");
}
