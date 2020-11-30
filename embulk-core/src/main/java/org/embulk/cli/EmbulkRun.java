package org.embulk.cli;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.embulk.EmbulkEmbed;
import org.embulk.EmbulkRunner;
import org.embulk.EmbulkSystemProperties;
import org.embulk.EmbulkVersion;
import org.embulk.jruby.LazyScriptingContainerDelegate;
import org.embulk.jruby.ScriptingContainerDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbulkRun {
    public static int run(final CommandLine commandLine, final EmbulkSystemProperties embulkSystemProperties) {
        return (new EmbulkRun()).runInternal(commandLine, embulkSystemProperties);
    }

    private int runInternal(final CommandLine commandLine, final EmbulkSystemProperties embulkSystemProperties) {
        final List<String> subcommandArguments = commandLine.getArguments();

        switch (commandLine.getCommand()) {
            case EXAMPLE:
                final EmbulkExample embulkExample = new EmbulkExample();
                try {
                    embulkExample.createExample(commandLine.getArguments().isEmpty()
                                                        ? "embulk-example"
                                                        : commandLine.getArguments().get(0));
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                    return 1;
                }
                return 0;
            case NEW:
                final String categoryWithLanguage = commandLine.getArguments().get(0);
                final String nameGiven = commandLine.getArguments().get(1);
                try {
                    final EmbulkNew embulkNew = new EmbulkNew(categoryWithLanguage, nameGiven, EmbulkVersion.VERSION);
                    embulkNew.newPlugin();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return 1;
                }
                return 0;
            case SELFUPDATE:
                if (commandLine.getArguments().isEmpty()) {
                    System.err.println("'embulk selfupdate' requires the target version since v0.10.0. It no longer updates to the latest version.");
                    return 1;
                }
                try {
                    SelfUpdate.toSpecific(
                            EmbulkVersion.VERSION,
                            commandLine.getArguments().get(0),
                            "true".equals(embulkSystemProperties.getProperty("force_selfupdate")));
                } catch (final IOException ex) {
                    ex.printStackTrace();
                    return 1;
                }
                return 0;
            case BUNDLE:
                if (!subcommandArguments.isEmpty() && subcommandArguments.get(0).equals("new")) {
                    System.err.println("embulk: 'embulk bundle new' is no longer available. Please use 'embulk mkbundle' instead.");
                    return -1;
                } else {
                    return runBundler(subcommandArguments, null, embulkSystemProperties);
                }
            case GEM:
                return callJRubyGem(subcommandArguments, embulkSystemProperties);
            case MKBUNDLE:
                newBundle(commandLine.getArguments().get(0), embulkSystemProperties);
                break;
            case RUN:
            case CLEANUP:
            case PREVIEW:
            case GUESS:
                // NOTE: When it was in Ruby "require 'embulk'" was required on top for Ruby
                // |Embulk::setup|.
                // Ruby |Embulk::setup| is now replaced with direct calls to EmbulkEmbed below.

                // TODO: Move this to initial JRuby instantiation.
                // reset context class loader set by org.jruby.Main.main to nil. embulk manages
                // multiple classloaders. default classloader should be
                // Plugin.class.getClassloader().
                Thread.currentThread().setContextClassLoader(null);

                // NOTE: When it was in Ruby ""require 'json'" was required.

                // NOTE: $LOAD_PATH and $CLASSPATH are set in JRubyInitializer via system config.

                // Ruby |Embulk::Runner| contained the EmbulkRunner instance, but it's no longer available.
                final EmbulkEmbed.Bootstrap bootstrap = new EmbulkEmbed.Bootstrap();
                bootstrap.setEmbulkSystemProperties(embulkSystemProperties);

                // see embulk-core/src/main/java/org/embulk/jruby/JRubyScriptingModule.
                final EmbulkRunner runner = new EmbulkRunner(bootstrap.initialize(), embulkSystemProperties);

                final Path configDiffPath = getPathFromProperties("config_diff_path", embulkSystemProperties);
                final Path outputPath = getPathFromProperties("output_path", embulkSystemProperties);
                final Path resumeStatePath = getPathFromProperties("resume_state_path", embulkSystemProperties);
                final String previewFormat = embulkSystemProperties.getProperty("preview_format");

                try {
                    switch (commandLine.getCommand()) {
                        case GUESS:
                            runner.guess(Paths.get(commandLine.getArguments().get(0)), outputPath);
                            break;
                        case PREVIEW:
                            runner.preview(Paths.get(commandLine.getArguments().get(0)), previewFormat);
                            break;
                        case RUN:
                            runner.run(Paths.get(commandLine.getArguments().get(0)),
                                       configDiffPath,
                                       outputPath,
                                       resumeStatePath);
                            break;
                        default:  // Do nothing, and just pass through.
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace(System.err);
                    System.err.println("");
                    System.err.println("Error: " + ex.getMessage());
                    return 1;
                }
                break;
            default:  // Do nothing, and just pass through.
        }
        return 0;
    }

    private static Path getPathFromProperties(final String key, final Properties properties) {
        final String value = properties.getProperty(key);
        if (value == null) {
            return null;
        }
        return Paths.get(value);
    }

    private void copyResourceToFile(final String sourceResourceName,
                                    final Path destinationBasePath,
                                    final String destinationRelativePath) throws IOException {
        final Path destinationPath = destinationBasePath.resolve(destinationRelativePath);
        System.out.println("  Creating " + destinationRelativePath);
        Files.createDirectories(destinationPath.getParent());
        Files.copy(EmbulkRun.class.getClassLoader().getResourceAsStream(sourceResourceName), destinationPath);
    }

    private int newBundle(final String pathString, final EmbulkSystemProperties embulkSystemProperties) {
        final Path path = Paths.get(pathString).toAbsolutePath();
        if (Files.exists(path)) {
            System.err.println("'" + pathString + "' already exists.");
            return 1;
        }

        System.out.println("Initializing " + pathString + "...");
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            ex.printStackTrace();
            return 1;
        }

        boolean success = false;
        try {
            // copy embulk/data/bundle/ contents
            copyResourceToFile("org/embulk/jruby/bundler/template/Gemfile", path, "Gemfile");
            // ".ruby-version" is no longer required since JRuby used is embedded in Embulk.
            copyResourceToFile("org/embulk/jruby/bundler/template/.bundle/config", path, ".bundle/config");
            copyResourceToFile("org/embulk/jruby/bundler/template/embulk/input/example.rb", path, "embulk/input/example.rb");
            copyResourceToFile("org/embulk/jruby/bundler/template/embulk/output/example.rb", path, "embulk/output/example.rb");
            copyResourceToFile("org/embulk/jruby/bundler/template/embulk/filter/example.rb", path, "embulk/filter/example.rb");
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

        int result = -1;
        final String bundlePath = embulkSystemProperties.getProperty("bundle_path");
        try {
            // run the first bundle-install
            result = runBundler(
                    Arrays.asList("install", "--path", bundlePath != null ? bundlePath : "."), path, embulkSystemProperties);
            if (result == 0) {
                success = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            if (!success) {
                try {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                                try {
                                    Files.deleteIfExists(file);
                                } catch (IOException ex) {
                                    // Ignore.
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exception) {
                                try {
                                    Files.deleteIfExists(dir);
                                } catch (IOException ex) {
                                    // Ignore.
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return 1;
                }
            }
        }
        return result;
    }

    private int runBundler(final List<String> arguments, final Path path, final EmbulkSystemProperties embulkSystemProperties) {
        final ScriptingContainerDelegate localJRubyContainer;
        try {
            localJRubyContainer = createJRubyForRubyCommand(embulkSystemProperties, "bundle");
        } catch (final NullPointerException ex) {
            // TODO: Handle the exception better and have a better error message.
            System.err.println(ex.getMessage());
            return -1;
        }

        localJRubyContainer.runScriptlet("require 'bundler'");  // bundler is included in embulk-core.jar

        // this hack is necessary to make --help working
        localJRubyContainer.runScriptlet("Bundler.define_singleton_method(:which_orig, Bundler.method(:which))");
        localJRubyContainer.runScriptlet("Bundler.define_singleton_method(:which) { |executable| (executable == 'man' ? false : which_orig(executable)) }");

        localJRubyContainer.runScriptlet("require 'bundler/friendly_errors'");
        localJRubyContainer.runScriptlet("require 'bundler/cli'");

        localJRubyContainer.put("__internal_argv_java__", arguments);
        if (path == null) {
            localJRubyContainer.runScriptlet("Bundler.with_friendly_errors { Bundler::CLI.start(Array.new(__internal_argv_java__), debug: true) }");
        } else {
            localJRubyContainer.put("__internal_working_dir__", path.toString());
            localJRubyContainer.runScriptlet(
                    "Dir.chdir(__internal_working_dir__) {"
                    + "  Bundler.with_friendly_errors { Bundler::CLI.start(Array.new(__internal_argv_java__), debug: true) }"
                    + "}");
            localJRubyContainer.remove("__internal_working_dir__");
        }
        localJRubyContainer.remove("__internal_argv_java__");

        return 0;
    }

    // TODO: Check if it is required to process JRuby options.
    private static ScriptingContainerDelegate createJRubyForRubyCommand(
            final EmbulkSystemProperties embulkSystemProperties,
            final String command) {
        final String propertyGemHome = embulkSystemProperties.getProperty("gem_home");
        if (propertyGemHome == null || propertyGemHome.isEmpty()) {
            logger.error("Embulk system property \"gem_home\" is not configured.");
            throw new NullPointerException("Embulk system property \"gem_home\" is not configured.");
        }
        final String propertyGemPath = embulkSystemProperties.getProperty("gem_path");  // gem_path is optional.

        final ScriptingContainerDelegate jruby = LazyScriptingContainerDelegate.withGemsIgnored(logger, embulkSystemProperties);

        if (jruby == null) {
            // TODO: Handle the exception better and have a better error message.
            throw new NullPointerException(
                    "JRuby is not configured well to run \"" + command + "\". Configure the Embulk system property \"jruby\".");
        }

        // The environment variables "GEM_HOME" (and "GEM_PATH") are mandatory for the "gem" and "bundle" commands.
        //
        // Unlike normal RubyGem loading, `Gem.paths.home=` does not work for the "gem" and "bundle" commands
        //
        // `Gem.paths` is overwritten in `GemRunner.new` in "rubygems".
        // https://github.com/rubygems/rubygems/blob/v3.1.4/lib/rubygems/gem_runner.rb#L80

        final String currentGemHome;
        if (jruby.callMethod(jruby.runScriptlet("ENV"), "has_key?", "GEM_HOME", Boolean.class)) {
            currentGemHome = jruby.callMethod(jruby.runScriptlet("ENV"), "fetch", "GEM_HOME", String.class);
        } else {
            currentGemHome = null;
        }
        if (currentGemHome == null || currentGemHome.isEmpty()) {
            logger.info(
                    "Environment variable \"GEM_HOME\" is not set. "
                    + "Setting \"GEM_HOME\" to \"{}\" from Embulk system property \"gem_home\" for the \"{}\" command.",
                    propertyGemHome, command);
            jruby.callMethod(jruby.runScriptlet("ENV"), "store", "GEM_HOME", propertyGemHome);
        } else if (!currentGemHome.equals(propertyGemHome)) {
            logger.info(
                    "Environment variable \"GEM_HOME\" is \"{}\", "
                    + "which is different from Embulk system property \"gem_home\". "
                    + "Resetting \"GEM_HOME\" to \"{}\" from \"gem_home\" for the \"{}\" command.",
                    currentGemHome, propertyGemHome, command);
            jruby.callMethod(jruby.runScriptlet("ENV"), "store", "GEM_HOME", propertyGemHome);
        }

        final String currentGemPath;
        if (jruby.callMethod(jruby.runScriptlet("ENV"), "has_key?", "GEM_PATH", Boolean.class)) {
            currentGemPath = jruby.callMethod(jruby.runScriptlet("ENV"), "fetch", "GEM_PATH", String.class);
        } else {
            currentGemPath = null;
        }
        jruby.callMethod(jruby.runScriptlet("ENV"), "store", "GEM_PATH", embulkSystemProperties.getProperty("gem_path"));
        if (currentGemPath == null || currentGemPath.isEmpty()) {
            if (propertyGemPath != null && !propertyGemPath.isEmpty()) {
                logger.info(
                        "Environment variable \"GEM_PATH\" is not set while Embulk system property \"gem_path\" is set. "
                        + "Setting \"GEM_PATH\" to \"{}\" from \"gem_path\" for the \"{}\" command.",
                        propertyGemPath, command);
                jruby.callMethod(jruby.runScriptlet("ENV"), "store", "GEM_PATH", propertyGemPath);
            }
        } else if (!currentGemPath.equals(propertyGemPath)) {
            if (propertyGemPath == null && propertyGemPath.isEmpty()) {
                logger.info(
                        "Environment variable \"GEM_PATH\" is \"{}\" while Embulk system property \"gem_path\" is not set. "
                        + "Unsetting \"GEM_PATH\" from \"gem_path\" for the \"{}\" command.",
                        currentGemPath, command);
                jruby.callMethod(jruby.runScriptlet("ENV"), "delete", "GEM_PATH");
            } else {
                logger.info(
                        "Environment variable \"GEM_PATH\" is \"{}\", "
                        + "which is different from Embulk system property \"gem_path\". "
                        + "Resetting \"GEM_PATH\" to \"{}\" from \"gem_path\" for the \"{}\" command.",
                        currentGemPath, propertyGemPath, command);
                jruby.callMethod(jruby.runScriptlet("ENV"), "store", "GEM_PATH", propertyGemPath);
            }
        }

        // NOTE: Same done in JRubyScriptingModule.
        // Remember to update |org.embulk.jruby.JRubyScriptingModule| when these environment variables are changed.
        if (jruby.callMethod(jruby.runScriptlet("ENV"), "has_key?", "BUNDLE_GEMFILE", Boolean.class)) {
            final String currentBundleGemFile =
                    jruby.callMethod(jruby.runScriptlet("ENV"), "fetch", "BUNDLE_GEMFILE", String.class);
            logger.warn(
                    "Environment variable \"BUNDLE_GEMFILE\" is set. Unsetting \"BUNDLE_GEMFILE\" for the \"{}\" command.",
                    command);
            jruby.callMethod(jruby.runScriptlet("ENV"), "delete", "BUNDLE_GEMFILE");
        }

        return jruby;
    }

    private int callJRubyGem(final List<String> subcommandArguments, final EmbulkSystemProperties embulkSystemProperties) {
        final ScriptingContainerDelegate localJRubyContainer;
        try {
            localJRubyContainer = createJRubyForRubyCommand(embulkSystemProperties, "gem");
        } catch (final NullPointerException ex) {
            // TODO: Handle the exception better and have a better error message.
            System.err.println(ex.getMessage());
            return -1;
        }

        localJRubyContainer.runScriptlet("require 'rubygems/gem_runner'");
        localJRubyContainer.put("__internal_argv_java__", subcommandArguments);
        localJRubyContainer.runScriptlet("Gem::GemRunner.new.run Array.new(__internal_argv_java__)");
        localJRubyContainer.remove("__internal_argv_java__");

        return 0;
    }

    private static final Logger logger = LoggerFactory.getLogger(EmbulkRun.class);
}
