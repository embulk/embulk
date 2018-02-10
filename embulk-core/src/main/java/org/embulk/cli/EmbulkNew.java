package org.embulk.cli;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.embulk.jruby.ScriptingContainerDelegateImpl;

public class EmbulkNew {
    public EmbulkNew(final String categoryWithLanguage, final String nameGiven, final String embulkVersion)
            throws IOException {
        this.basePath = Paths.get(".").toAbsolutePath();

        final LanguageAndCategory languageAndCategory = LanguageAndCategory.of(categoryWithLanguage);
        this.language = languageAndCategory.getLanguage();
        this.category = languageAndCategory.getCategory();
        this.nameGiven = nameGiven;
        this.embulkVersion = embulkVersion;

        if (category.equals("file_input")) {
            this.embulkCategory = "input";
        } else if (category.equals("file_output")) {
            this.embulkCategory = "output";
        } else {
            this.embulkCategory = category;
        }

        this.name = nameGiven.replaceAll("[^a-zA-Z0-9_]+", "_");

        this.fullProjectName = "embulk-" + embulkCategory + "-" + name;
        this.pluginDirectory = "lib/embulk";
        this.pluginPath = pluginDirectory + "/" + embulkCategory + "/" + name + ".rb";

        this.pluginBasePath = this.basePath.resolve(fullProjectName);

        this.velocityEngine = new VelocityEngine();
        this.velocityEngine.init();
        this.velocityEngine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS,
                                        "org.apache.velocity.runtime.log.NullLogSystem");
    }

    public boolean newPlugin() throws IOException {
        if (Files.exists(this.pluginBasePath)) {
            throw new IOException("./" + this.fullProjectName + " already exists. Please delete it first.");
        }

        Files.createDirectories(this.pluginBasePath);

        System.out.println("Creating " + this.fullProjectName + "/");

        boolean success = false;
        try {
            //
            // Generate gemspec
            //
            final String author = getGitConfig("user.name", "YOUR_NAME");
            final String email = getGitConfig("user.email", "YOUR_NAME");
            final String expectedGitHubAccount = email.split("@")[0];

            // variables used in Velocity templates
            final String rubyClassName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
            final String javaClassName =
                    CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name)
                    + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, category)
                    + "Plugin";
            final String javaPackageName = "org.embulk." + embulkCategory + "." + name;
            final String displayName = getDisplayName(name);
            final String displayCategory = category.replace("_", " ");

            final HashMap<String, String> extraGuesses = new HashMap<String, String>();

            final String description;
            switch (category) {
                case "input":
                    description = String.format("Loads records from %s.", displayName);
                    break;
                case "file_input":
                    description = String.format("Reads files stored on %s.", displayName);
                    break;
                case "parser":
                    description = String.format("Parses %s files read by other file input plugins.", displayName);
                    extraGuesses.put("org/embulk/plugin/template/ruby/parser_guess.rb.vm",
                                     String.format("%s/guess/%s.rb", pluginDirectory, name));
                    break;
                case "decoder":
                    description = String.format("Decodes %s-encoded files read by other file input plugins.", displayName);
                    extraGuesses.put("org/embulk/plugin/template/ruby/decoder_guess.rb.vm",
                                     String.format("%s/guess/%s.rb", pluginDirectory, name));
                    break;
                case "output":
                    description = String.format("Dumps records to %s.", displayName);
                    break;
                case "file_output":
                    description = String.format("Stores files on %s.", displayName);
                    break;
                case "formatter":
                    description = String.format("Formats %s files for other file output plugins.", displayName);
                    break;
                case "encoder":
                    description = String.format("Encodes files using %s for other file output plugins.", displayName);
                    break;
                case "filter":
                    description = String.format("%s", displayName);
                    break;
                default:
                    throw new RuntimeException("FATAL: Invalid plugin category.");
            }

            //
            // Generate project repository
            //
            final VelocityContext velocityContext = createVelocityContext(
                    author,
                    category,
                    description,
                    displayName,
                    displayCategory,
                    email,
                    embulkCategory,
                    this.embulkVersion,
                    expectedGitHubAccount,
                    fullProjectName,
                    javaClassName,
                    javaPackageName,
                    ScriptingContainerDelegateImpl.getJRubyVersion(EmbulkNew.class.getClassLoader()),
                    language,
                    name,
                    rubyClassName);
            copyTemplated("org/embulk/plugin/template/README.md.vm", "README.md", velocityContext);
            copy("org/embulk/plugin/template/LICENSE.txt", "LICENSE.txt");
            copyTemplated("org/embulk/plugin/template/gitignore.vm", ".gitignore", velocityContext);

            switch (language) {
                case "ruby":
                    copy("org/embulk/plugin/template/ruby/Rakefile", "Rakefile");
                    copy("org/embulk/plugin/template/ruby/Gemfile", "Gemfile");
                    copyTemplated("org/embulk/plugin/template/ruby/.ruby-version",
                                  ".ruby-version",
                                  velocityContext);
                    copyTemplated("org/embulk/plugin/template/ruby/gemspec.vm",
                                  fullProjectName + ".gemspec",
                                  velocityContext);
                    copyTemplated(String.format("org/embulk/plugin/template/ruby/%s.rb.vm", category),
                                  this.pluginPath,
                                  velocityContext);
                    break;
                case "java":
                    copy("org/embulk/plugin/template/java/gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.jar");
                    copy("org/embulk/plugin/template/java/gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.properties");
                    copy("org/embulk/plugin/template/java/gradlew.bat", "gradlew.bat");
                    copy("org/embulk/plugin/template/java/gradlew", "gradlew");
                    setExecutable("gradlew");
                    copy("org/embulk/plugin/template/java/config/checkstyle/checkstyle.xml", "config/checkstyle/checkstyle.xml");
                    copy("org/embulk/plugin/template/java/config/checkstyle/default.xml", "config/checkstyle/default.xml");
                    copyTemplated("org/embulk/plugin/template/java/build.gradle.vm",
                                  "build.gradle",
                                  velocityContext);
                    copyTemplated("org/embulk/plugin/template/java/plugin_loader.rb.vm",
                                  this.pluginPath,
                                  velocityContext);
                    copyTemplated(String.format("org/embulk/plugin/template/java/%s.java.vm", category),
                                  String.format("src/main/java/%s/%s.java",
                                                javaPackageName.replaceAll("\\.", "/"),
                                                javaClassName),
                                  velocityContext);
                    copyTemplated("org/embulk/plugin/template/java/test.java.vm",
                                  String.format("src/test/java/%s/Test%s.java",
                                                javaPackageName.replaceAll("\\.", "/"),
                                                javaClassName),
                                  velocityContext);
                    break;
                default:
                    throw new RuntimeException("FATAL: Invalid plugin language.");
            }

            for (Map.Entry<String, String> entry : extraGuesses.entrySet()) {
                copyTemplated(entry.getKey(), entry.getValue(), velocityContext);
            }

            System.out.println("");
            System.out.println("Plugin template is successfully generated.");

            switch (language) {
                case "ruby":
                    System.out.println("Next steps:");
                    System.out.println("");
                    System.out.printf("  $ cd %s\n", fullProjectName);
                    System.out.println("  $ bundle install                      # install one using rbenv & rbenv-build");
                    System.out.println("  $ bundle exec rake                    # build gem to be released");
                    System.out.println("  $ bundle exec embulk run config.yml   # you can run plugin using this command");
                    break;
                case "java":
                    System.out.println("Next steps:");
                    System.out.println("");
                    System.out.printf("  $ cd %s\n", fullProjectName);
                    System.out.println("  $ ./gradlew package");
                    break;
                default:
                    throw new RuntimeException("FATAL: Invalid plugin language.");
            }

            success = true;
            System.out.println("");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (!success) {
                System.out.println("Failed. Removing the directory created.");
                deleteDirectoryTree(Paths.get(fullProjectName));
            }
        }
        return success;
    }

    private static class LanguageAndCategory {
        private LanguageAndCategory(final String language, final String category) {
            this.language = language;
            this.category = category;
        }

        public static LanguageAndCategory of(final String categoryWithLanguage) {
            switch (categoryWithLanguage) {
                case "java-input":
                    return new LanguageAndCategory("java", "input");
                case "java-output":
                    return new LanguageAndCategory("java", "output");
                case "java-filter":
                    return new LanguageAndCategory("java", "filter");
                case "java-file-input":
                    return new LanguageAndCategory("java", "file_input");
                case "java-file-output":
                    return new LanguageAndCategory("java", "file_output");
                case "java-parser":
                    return new LanguageAndCategory("java", "parser");
                case "java-formatter":
                    return new LanguageAndCategory("java", "formatter");
                case "java-decoder":
                    return new LanguageAndCategory("java", "decoder");
                case "java-encoder":
                    return new LanguageAndCategory("java", "encoder");
                case "ruby-input":
                    return new LanguageAndCategory("ruby", "input");
                case "ruby-output":
                    return new LanguageAndCategory("ruby", "output");
                case "ruby-filter":
                    return new LanguageAndCategory("ruby", "filter");
                case "ruby-file-input":
                    throw new RuntimeException("ruby-file-input is not implemented yet. See #21 on github.");
                case "ruby-file-output":
                    throw new RuntimeException("ruby-file-output is not implemented yet. See #22 on github.");
                case "ruby-parser":
                    return new LanguageAndCategory("ruby", "parser");
                case "ruby-formatter":
                    return new LanguageAndCategory("ruby", "formatter");
                case "ruby-decoder":
                    throw new RuntimeException("ruby-decoder is not implemented yet. See #31 on github.");
                case "ruby-encoder":
                    throw new RuntimeException("ruby-decoder is not implemented yet. See #32 on github.");
                default:
                    throw new RuntimeException(String.format("Unknown category '%s'", categoryWithLanguage));
            }
        }

        public String getLanguage() {
            return this.language;
        }

        public String getCategory() {
            return this.category;
        }

        private final String language;
        private final String category;
    }

    private String getGitConfig(final String configName, final String defaultValue) {
        try {
            final Process process = new ProcessBuilder("git", "config", configName).redirectErrorStream(true).start();
            return CharStreams.toString(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)).trim();
        } catch (Throwable ex) {
            return "YOUR_NAME";
        }
    }

    private Path deleteDirectoryTree(final Path path) throws IOException {
        return Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
    }

    private String getDisplayName(final String name) {
        final String[] nameSplit = name.split("_");
        final ArrayList<String> nameComposition = new ArrayList<String>();
        for (String namePart : nameSplit) {
            nameComposition.add(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, namePart));
        }
        return Joiner.on(" ").join(nameComposition);
    }

    private VelocityContext createVelocityContext(
            final String author,
            final String category,
            final String description,
            final String displayName,
            final String displayCategory,
            final String email,
            final String embulkCategory,
            final String embulkVersion,
            final String expectedGitHubAccount,
            final String fullProjectName,
            final String javaClassName,
            final String javaPackageName,
            final String jrubyVersion,
            final String language,
            final String name,
            final String rubyClassName) {
        final VelocityContext velocityContext = new VelocityContext();
        // TODO(dmikurube): Revisit this |argumentToRunEmbulkJava|.
        // This is in the Velocity context because the value could not be in Velocity templates.
        velocityContext.put("argumentToRunEmbulkJava", "\'-L ${file(\".\").absolutePath}\'");
        velocityContext.put("author", author);
        velocityContext.put("category", category);
        velocityContext.put("description", description);
        velocityContext.put("displayName", displayName);
        velocityContext.put("displayCategory", displayCategory);
        velocityContext.put("email", email);
        velocityContext.put("embulkCategory", embulkCategory);
        velocityContext.put("embulkVersion", embulkVersion);
        velocityContext.put("expectedGitHubAccount", expectedGitHubAccount);
        velocityContext.put("fullProjectName", fullProjectName);
        velocityContext.put("javaClassName", javaClassName);
        velocityContext.put("javaGuessClassName", javaClassName.replace("Plugin", "GuessPlugin"));
        velocityContext.put("javaPackageName", javaPackageName);
        velocityContext.put("jrubyVersion", jrubyVersion);
        velocityContext.put("language", language);
        velocityContext.put("name", name);
        velocityContext.put("rubyClassName", rubyClassName);
        velocityContext.put("rubyGuessClassName", rubyClassName.replace("Plugin", "GuessPlugin"));
        return velocityContext;
    }

    private void copy(String sourceResourcePath, String destinationFileName) throws IOException {
        final Path destinationPath = this.pluginBasePath.resolve(destinationFileName);
        Files.createDirectories(destinationPath.getParent());
        Files.copy(EmbulkNew.class.getClassLoader().getResourceAsStream(sourceResourcePath), destinationPath);
    }

    private void copyTemplated(String sourceResourcePath, String destinationFileName, VelocityContext velocityContext)
            throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                     EmbulkNew.class.getClassLoader().getResourceAsStream(sourceResourcePath))) {
            final Path destinationPath = this.pluginBasePath.resolve(destinationFileName);
            Files.createDirectories(destinationPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(destinationPath, StandardCharsets.UTF_8)) {
                this.velocityEngine.evaluate(velocityContext, writer, "embulk-new", reader);
            }
        }
    }

    private void setExecutable(String targetFileName) throws IOException {
        final Path targetPath = this.pluginBasePath.resolve(targetFileName);
        final Set<PosixFilePermission> permissions =
                new HashSet<PosixFilePermission>(Files.getPosixFilePermissions(targetPath));
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        permissions.add(PosixFilePermission.GROUP_EXECUTE);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        Files.setPosixFilePermissions(targetPath, permissions);
    }

    private final Path basePath;

    private final String nameGiven;
    private final String language;
    private final String category;
    private final String embulkVersion;

    private final String embulkCategory;
    private final String name;

    private final String fullProjectName;
    private final String pluginDirectory;
    private final String pluginPath;

    private final Path pluginBasePath;

    private final VelocityEngine velocityEngine;
}
