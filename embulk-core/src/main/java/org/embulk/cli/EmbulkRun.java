package org.embulk.cli;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.embulk.EmbulkEmbed;
import org.embulk.EmbulkRunner;
import org.embulk.EmbulkVersion;
import org.embulk.jruby.ScriptingContainerDelegate;
import org.embulk.jruby.ScriptingContainerDelegateImpl;

public class EmbulkRun {
    public static int run(final CommandLine commandLine) {
        return (new EmbulkRun()).runInternal(commandLine);
    }

    private int runInternal(final CommandLine commandLine) {
        final List<String> subcommandArguments = commandLine.getArguments();
        final Properties embulkSystemProperties = commandLine.getCommandLineProperties();

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
                } else {
                    runBundler(subcommandArguments, null);
                }
                return 0;
            case GEM:
                callJRubyGem(subcommandArguments);
                return 0;
            case MKBUNDLE:
                newBundle(commandLine.getArguments().get(0), embulkSystemProperties.getProperty("bundle_Path"));
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
                final EmbulkRunner runner = new EmbulkRunner(bootstrap.initialize());

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

    private int newBundle(final String pathString, final String bundlePath) {
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

        try {
            // run the first bundle-install
            runBundler(Arrays.asList("install", "--path", bundlePath != null ? bundlePath : "."), path);
            success = true;
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
        return 0;
    }

    private void runBundler(final List<String> arguments, final Path path) {
        final ScriptingContainerDelegate localJRubyContainer = createLocalJRubyScriptingContainerDelegate();
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
            localJRubyContainer.runScriptlet("Dir.chdir(__internal_working_dir__) { Bundler.with_friendly_errors { Bundler::CLI.start(__internal_argv__, debug: true) } }");
            localJRubyContainer.remove("__internal_working_dir__");
        }
        localJRubyContainer.remove("__internal_argv_java__");
    }

    // TODO: Check if it is required to process JRuby options.
    @SuppressWarnings("checkstyle:LineLength")
    private ScriptingContainerDelegate createLocalJRubyScriptingContainerDelegate() {
        // Not |LocalContextScope.SINGLETON| to narrow down considerations.
        final ScriptingContainerDelegate jruby = ScriptingContainerDelegateImpl.create(
                EmbulkRun.class.getClassLoader(),
                ScriptingContainerDelegate.LocalContextScope.SINGLETHREAD,
                ScriptingContainerDelegate.LocalVariableBehavior.PERSISTENT);

        // NOTE: Same done in JRubyScriptingModule.
        // Remember to update |org.embulk.jruby.JRubyScriptingModule| when these environment variables are changed.
        final boolean hasBundleGemfile =
                jruby.callMethod(jruby.runScriptlet("ENV"), "has_key?", "BUNDLE_GEMFILE", Boolean.class);
        if (hasBundleGemfile) {
            final String bundleGemFile =
                    jruby.callMethod(jruby.runScriptlet("ENV"), "fetch", "BUNDLE_GEMFILE", String.class);
            System.err.println("BUNDLE_GEMFILE has already been set: \"" + bundleGemFile + "\"");
            System.err.println("BUNDLE_GEMFILE is being unset.");
            jruby.callMethod(jruby.runScriptlet("ENV"), "delete", "BUNDLE_GEMFILE");
        }

        // `Gem.paths` does not work for "gem" and "bundle". The environment variables are required.
        jruby.callMethod(jruby.runScriptlet("ENV"), "store", "GEM_HOME", this.buildDefaultGemPath());
        jruby.callMethod(jruby.runScriptlet("ENV"), "delete", "GEM_PATH");

        return jruby;
    }

    private void callJRubyGem(final List<String> subcommandArguments) {
        final ScriptingContainerDelegate localJRubyContainer = createLocalJRubyScriptingContainerDelegate();

        localJRubyContainer.runScriptlet("puts ''");
        localJRubyContainer.runScriptlet("puts 'Gem plugin path is: %s' % (ENV.has_key?('GEM_HOME') ? ENV['GEM_HOME'] : '(empty)')");
        localJRubyContainer.runScriptlet("puts ''");

        localJRubyContainer.runScriptlet("require 'rubygems/gem_runner'");
        localJRubyContainer.put("__internal_argv_java__", subcommandArguments);
        localJRubyContainer.runScriptlet("Gem::GemRunner.new.run Array.new(__internal_argv_java__)");
        localJRubyContainer.remove("__internal_argv_java__");
    }

    private String buildDefaultGemPath() {
        return this.buildEmbulkHome().resolve("lib").resolve("gems").toString();
    }

    // TODO: Manage the "home directory" in one place in a configurable way.
    // https://github.com/embulk/embulk/issues/910
    private Path buildEmbulkHome() {
        final String userHomeProperty = System.getProperty("user.home");

        if (userHomeProperty == null) {
            throw new RuntimeException("User home directory is not set in Java properties.");
        }

        final Path userHome;
        try {
            userHome = Paths.get(userHomeProperty);
        } catch (InvalidPathException ex) {
            throw new RuntimeException("User home directory is invalid: \"" + userHomeProperty + "\"", ex);
        }

        return userHome.toAbsolutePath().resolve(".embulk");
    }
}
