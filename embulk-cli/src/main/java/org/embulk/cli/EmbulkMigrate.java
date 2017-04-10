package org.embulk.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import org.apache.maven.artifact.versioning.ComparableVersion;

public class EmbulkMigrate
{
    public void migratePlugin(final String pathInString, final String thisEmbulkVersion)
            throws IOException
    {
        migratePlugin(Paths.get(pathInString), thisEmbulkVersion);
    }

    public void migratePlugin(final Path path, final String thisEmbulkVersion)
            throws IOException
    {
        final Migrator migrator = new Migrator(path);

        List<Matcher> matchedEmbulkCoreInGradle = migrator.matchers("**/build.gradle", EMBULK_CORE_IN_GRADLE);
        List<Matcher> matchedNewEmbulkInGemspec = migrator.matchers("**/*.gemspec", NEW_EMBULK_IN_GEMSPEC);
        List<Matcher> matchedOldEmbulkInGemspec = migrator.matchers("**/*.gemspec", OLD_EMBULK_IN_GEMSPEC);

        final Language language;
        final ComparableVersion fromVersion;
        if (!matchedEmbulkCoreInGradle.isEmpty()) {
            language = Language.JAVA;
            fromVersion = new ComparableVersion(matchedEmbulkCoreInGradle.get(0).group(1).replace("+", "0"));
            System.out.printf("Detected Java plugin for Embulk %s...\n", fromVersion.toString());
        }
        else if (!matchedNewEmbulkInGemspec.isEmpty()) {
            language = Language.RUBY;
            fromVersion = new ComparableVersion(matchedNewEmbulkInGemspec.get(0).group(1));
            System.out.printf("Detected Ruby plugin for Embulk %s...\n", fromVersion.toString());
        }
        else if (!matchedOldEmbulkInGemspec.isEmpty()) {
            language = Language.RUBY;
            fromVersion = new ComparableVersion("0.1.0");
            System.out.println("Detected Ruby plugin for unknown Embulk version...");
        }
        else {
            throw new RuntimeException("Failed to detect plugin language and dependency version");
        }

        switch (language) {
        case JAVA:
            migrateJavaPlugin(migrator, fromVersion, thisEmbulkVersion);
            break;
        case RUBY:
            migrateRubyPlugin(migrator, fromVersion, thisEmbulkVersion);
            break;
        }

        if (migrator.getModifiedFiles().isEmpty()) {
            System.out.println("Done. No files are modified.");
        }
        else {
            System.out.println("Done. Please check modifieid files.");
        }
    }

    private void migrateJavaPlugin(final Migrator migrator,
                                   final ComparableVersion fromVersion,
                                   final String thisEmbulkVersion)
                throws IOException
    {
        if (fromVersion.compareTo(new ComparableVersion("0.7.0")) < 0) {
            // rename CommitReport to TaskReport
            migrator.replace("**/*.java", Pattern.compile("(CommitReport)"), 1, "TaskReport");
            migrator.replace("**/*.java", Pattern.compile("(commitReport)"), 1, "taskReport");
        }

        // upgrade gradle version
        if (migrator.match("gradle/wrapper/gradle-wrapper.properties", GRADLE_VERSION_IN_WRAPPER)) {
            // gradle < 3.2.1
            migrator.copy("embulk/data/new/java/gradle/wrapper/gradle-wrapper.properties",
                          "gradle/wrapper/gradle-wrapper.properties");
            migrator.copy("embulk/data/new/java/gradle/wrapper/gradle-wrapper.jar",
                          "gradle/wrapper/gradle-wrapper.jar");
        }

        // Add a method |jsonColumn| before the method |timestampColumn| which should exist.
        if (!migrator.match("**/*.java", JSON_COLUMN_METHOD_IN_ALL_JAVA)) {
            final List<Matcher> matchers = migrator.matchers("**/*.java", TIMESTAMP_COLUMN_METHOD_IN_ALL_JAVA);
            final String indent = matchers.get(0).group(1);
            final String JSON_COLUMN_METHOD = Joiner.on("\n").join(
                "",
                indent + "public void jsonColumn(Column column) {",
                indent + "    throw new UnsupportedOperationException(\"This plugin doesn't support json type. Please try to upgrade version of the plugin using 'embulk gem update' command. If the latest version still doesn't support json type, please contact plugin developers, or change configuration of input plugin not to use json type.\");",
                indent + "}",
                "",
                indent + "@Override",
                "");
            migrator.replace("**/*.java", TIMESTAMP_COLUMN_METHOD_AFTER_NEWLINE_IN_ALL_JAVA, 1, JSON_COLUMN_METHOD);
        }

        // Add |sourceCompatibility| and |targetCompatibility| in build.gradle before |dependencies| existing.
        if (!migrator.match("build.gradle", TARGET_COMPATIBILITY_IN_GRADLE)) {
            migrator.insertLine("build.gradle", DEPENDENCIES_IN_GRADLE, new StringUpsert() {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        return String.format("%stargetCompatibility = 1.7\n", matcher.group(1));
                    }
                });
        }
        if (!migrator.match("build.gradle", SOURCE_COMPATIBILITY_IN_GRADLE)) {
            migrator.insertLine("build.gradle", DEPENDENCIES_IN_GRADLE, new StringUpsert() {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        return String.format("%ssourceCompatibility = 1.7\n", matcher.group(1));
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
            migrator.copy("embulk/data/new/java/config/checkstyle/checkstyle.xml",
                          "config/checkstyle/checkstyle.xml");
        }

        // Add |checkstyle| settings before the |gem| task existing.
        if (!migrator.match("build.gradle", CHECKSTYLE_CONFIGURATION_IN_GRADLE)) {
            migrator.copy("embulk/data/new/java/config/checkstyle/default.xml",
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
        migrator.replace("**/build.gradle", EMBULK_CORE_OR_STANDARDS_IN_GRADLE, 1, thisEmbulkVersion);
    }

    private void migrateRubyPlugin(final Migrator migrator,
                                   final ComparableVersion fromVersion,
                                   final String thisEmbulkVersion)
            throws IOException
    {
        migrator.write(".ruby-version", "jruby-9.1.5.0");

        // Update |embulk| version depending.
        if (fromVersion.compareTo(new ComparableVersion("0.1.0")) <= 0) {
            // Add add_development_dependency.
            migrator.insertLine("**/*.gemspec", DEVELOPMENT_DEPENDENCY_IN_GEMSPEC, new StringUpsert() {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        return String.format("%s.add_development_dependency 'embulk', ['>= %s']",
                                             matcher.group(1),
                                             thisEmbulkVersion);
                    }
                });
        }
        else {
            if (migrator.replace("**/*.gemspec", EMBULK_DEPENDENCY_PRERELEASE_IN_GEMSPEC, 1,
                                 ">= " + thisEmbulkVersion).isEmpty()) {
                migrator.replace("**/*.gemspec", EMBULK_DEPENDENCY_IN_GEMSPEC, 1, thisEmbulkVersion);
            }
        }
    }

    private class Migrator {
        private Migrator(Path basePath) {
            this.basePath = basePath;
            this.modifiedFiles = new HashSet<Path>();
        }

        public Path getBasePath()
        {
            return this.basePath;
        }

        public Set<Path> getModifiedFiles()
        {
            return this.modifiedFiles;
        }

        public void copy(String sourceResourcePath, String destinationFileName)
                throws IOException
        {
            Path destinationPath = this.basePath.resolve(destinationFileName);
            Files.createDirectories(destinationPath.getParent());
            Files.copy(EmbulkMigrate.class.getClassLoader().getResourceAsStream(sourceResourcePath), destinationPath);
        }

        public boolean match(String glob, String pattern)
                throws IOException
        {
            return !matchers(glob, Pattern.compile(pattern)).isEmpty();
        }

        public boolean match(String glob, Pattern pattern)
                throws IOException
        {
            return !matchers(glob, pattern).isEmpty();
        }

        public List<Matcher> matchers(String glob, String pattern)
                throws IOException
        {
            return matchers(glob, Pattern.compile(pattern));
        }

        public List<Matcher> matchers(String glob, Pattern pattern)
                throws IOException
        {
            ImmutableList.Builder<Matcher> matchers = ImmutableList.<Matcher>builder();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.basePath, glob)) {
                for (Path filePath : directoryStream) {
                    final Matcher matcher = pattern.matcher(read(filePath));
                    if (matcher.matches()) {
                        matchers.add(matcher);
                    }
                }
            }
            return matchers.build();
        }

        public final List<Matcher> replace(String glob, Pattern pattern, int index, final String immediate)
                throws IOException
        {
            return replace(glob, pattern, index, new StringUpsert()
                {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        return immediate;
                    }
                });
        }

        public List<Matcher> replace(String glob, Pattern pattern, int index, StringUpsert stringUpsert)
                throws IOException
        {
            ImmutableList.Builder<Matcher> matchers = ImmutableList.<Matcher>builder();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.basePath, glob)) {
                for (Path filePath : directoryStream) {
                    final String originalData = read(filePath);
                    Matcher first = null;
                    int position = 0;
                    String modifiedData = originalData;
                    while (position < modifiedData.length()) {
                        final Matcher matcher = pattern.matcher(modifiedData.substring(position));
                        if (!matcher.matches()) {
                            break;
                        }
                        if (first == null) {
                            first = matcher;
                        }
                        final String replacingString = stringUpsert.getUpsertd(matcher);
                        modifiedData =
                            modifiedData.substring(0, matcher.start(index) - 1) +
                            replacingString +
                            modifiedData.substring(matcher.end(index));
                        position =
                            matcher.start(index) +
                            replacingString.length() +
                            (matcher.end() - matcher.end(index));
                    }
                    if (first != null) {
                        modify(filePath, modifiedData);
                    }
                    matchers.add(first);
                }
            }
            return matchers.build();
        }

        public final List<Matcher> insertLine(String glob, String pattern, StringUpsert stringUpsert)
                throws IOException
        {
            return insertLine(glob, Pattern.compile(pattern), stringUpsert);
        }

        public final List<Matcher> insertLine(String glob, Pattern pattern, final String immediate)
                throws IOException
        {
            return insertLine(glob, pattern, new StringUpsert()
                {
                    @Override
                    public String getUpsertd(Matcher matcher) {
                        return immediate;
                    }
                });
        }

        public List<Matcher> insertLine(String glob, Pattern pattern, StringUpsert stringUpsert)
                throws IOException
        {
            ImmutableList.Builder<Matcher> matchers = ImmutableList.<Matcher>builder();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.basePath, glob)) {
                for (Path filePath : directoryStream) {
                    final String originalData = read(filePath);
                    final Matcher matcher = pattern.matcher(originalData);
                    if (matcher.matches()) {
                        final String preMatch = originalData.substring(0, matcher.start());
                        final int lineNumber = preMatch.split("\n").length;
                        final String replacingString = stringUpsert.getUpsertd(matcher);
                        List<String> lines = new ArrayList<String>(Arrays.asList(originalData.split("\n")));
                        lines.add(lineNumber + 1, replacingString);
                        final String modifiedData = Joiner.on("\n").join(lines);
                        modify(filePath, modifiedData);
                        matchers.add(matcher);
                    }
                }
            }
            return matchers.build();
        }

        public void write(String fileName, String writtenData)
                throws IOException
        {
            Path destinationPath = this.basePath.resolve(fileName);
            Files.createDirectories(destinationPath.getParent());
            modify(destinationPath, writtenData);
        }

        private void modify(Path filePath, String modifiedData)
                throws IOException
        {
            final String originalData = read(filePath);
            if (!originalData.equals(modifiedData)) {
                Files.write(filePath, modifiedData.getBytes(StandardCharsets.UTF_8));
                if (!this.modifiedFiles.contains(filePath)) {
                    if (originalData.isEmpty()) {
                        System.out.printf("  Created %s\n", filePath.toString());
                    }
                    else {
                        System.out.printf("  Modified %s\n", filePath.toString());
                    }
                    this.modifiedFiles.add(filePath);
                }
            }
        }

        private String read(Path filePath)
                throws IOException
        {
            // assumes source code is written in UTF-8.
            return new String(readBytes(filePath), StandardCharsets.UTF_8);
        }

        private byte[] readBytes(Path filePath)
                throws IOException
        {
            return Files.readAllBytes(filePath);
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
        "gradle-[23]\\.\\d+(\\.\\d+)?-/");
    private static final Pattern JSON_COLUMN_METHOD_IN_ALL_JAVA = Pattern.compile(
        "void\\s+jsonColumn");
    private static final Pattern TIMESTAMP_COLUMN_METHOD_IN_ALL_JAVA = Pattern.compile(
        "^(\\W+).*?void\\s+timestampColumn");
    private static final Pattern TIMESTAMP_COLUMN_METHOD_AFTER_NEWLINE_IN_ALL_JAVA = Pattern.compile(
        "(\\r?\\n)(\\W+).*?void\\s+timestampColumn");
    private static final Pattern TARGET_COMPATIBILITY_IN_GRADLE = Pattern.compile(
        "targetCompatibility");
    private static final Pattern SOURCE_COMPATIBILITY_IN_GRADLE = Pattern.compile(
        "sourceCompatibility");
    private static final Pattern DEPENDENCIES_IN_GRADLE = Pattern.compile(
        "^([ \\t]*)dependencies\\s*{");
    private static final Pattern CHECKSTYLE_PLUGIN_IN_GRADLE = Pattern.compile(
        "id\\s+(?<quote>[\"\'])checkstyle\\k<quote>");
    private static final Pattern JAVA_PLUGIN_IN_GRADLE = Pattern.compile(
        "^([ \t]*)id( +)([\"\'])java[\"\']");
    private static final Pattern CHECKSTYLE_CONFIGURATION_IN_GRADLE = Pattern.compile(
        "checkstyle\\s+{");
    private static final Pattern GEM_TASK_IN_GRADLE = Pattern.compile(
        "^([ \\t]*)task\\s+gem\\W.*{");
    private static final Pattern EMBULK_CORE_OR_STANDARDS_IN_GRADLE = Pattern.compile(
        "org\\.embulk:embulk-(?:core|standards):([\\d\\.\\+]+)?");
    private static final Pattern DEVELOPMENT_DEPENDENCY_IN_GEMSPEC = Pattern.compile(
        "([ \\t]*\\w+)\\.add_development_dependency");
    private static final Pattern EMBULK_DEPENDENCY_PRERELEASE_IN_GEMSPEC = Pattern.compile(
        "add_(?:development_)?dependency\\s+\\W+embulk\\W+\\s*(\\~\\>\\s*[\\d\\.]+)\\W+");
    private static final Pattern EMBULK_DEPENDENCY_IN_GEMSPEC = Pattern.compile(
        "add_(?:development_)?dependency\\s+\\W+embulk\\W+\\s*([\\d\\.]+)\\W+");
}
