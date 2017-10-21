package org.embulk.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.embulk.EmbulkRunner;
import org.embulk.EmbulkSetup;
import org.embulk.cli.parse.EmbulkCommandLineHelpRequired;
import org.embulk.cli.parse.EmbulkCommandLineParseException;
import org.embulk.cli.parse.EmbulkCommandLineParser;
import org.embulk.cli.parse.OptionBehavior;
import org.embulk.cli.parse.OptionDefinition;
// TODO: Replace org.joda.time with java.time when Embulk goes to Java 8.
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

public class EmbulkRun
{
    public EmbulkRun(final String embulkVersion)
    {
        this.embulkVersion = embulkVersion;
    }

    public int run(final List<String> argsEmbulk, final List<String> jrubyOptions)
    {
        final EmbulkArguments arguments;
        try {
            arguments = EmbulkArguments.extract(argsEmbulk);
        }
        catch (EmbulkCommandLineException ex) {
            printGeneralUsage(System.err);
            System.err.println("");
            System.err.println("error: " + ex.getMessage());
            return 1;
        }

        final EmbulkSubcommand subcommand = arguments.getSubcommand();
        if (subcommand == null) {
            printGeneralUsage(System.err);
            System.err.println("");
            System.err.println("Use `<command> --help` to see description of the commands.");
            return 1;
        }

        final List<String> subcommandArguments = arguments.getSubcommandArguments();

        switch (subcommand) {
        case VERSION_OUT:
            // TODO(v2)[#723]: Consider capitalizing this "embulk".
            // https://github.com/embulk/embulk/issues/723
            System.out.println("embulk " + this.embulkVersion);
            return 0;
        case VERSION_ERR:
            // TODO(v2)[#723]: Consider capitalizing this "embulk".
            // https://github.com/embulk/embulk/issues/723
            System.err.println("embulk " + this.embulkVersion);
            return 0;
        }

        printEmbulkVersionHeader(System.out);
        printEmbulkGeneralNotifications(System.out);

        switch (subcommand) {
        case BUNDLE:
        case EXEC:
        case GEM:
        case IRB:
            return runSubcommand(subcommand, subcommandArguments, null, jrubyOptions);
        default:
            final EmbulkCommandLineParser parser = buildCommandLineParser(subcommand);
            final EmbulkCommandLine commandLine;
            try {
                commandLine = parser.parse(
                    subcommandArguments, jrubyOptions, new PrintWriter(System.out), new PrintWriter(System.err));
            }
            catch (EmbulkCommandLineParseException ex) {
                parser.printHelp(System.err);
                System.err.println("");
                System.err.println(ex.getMessage());
                return 1;
            }
            catch (EmbulkCommandLineHelpRequired ex) {
                parser.printHelp(System.err);
                return 1;
            }
            return runSubcommand(subcommand, subcommandArguments, commandLine, jrubyOptions);
        }
    }

    private EmbulkCommandLineParser buildCommandLineParser(final EmbulkSubcommand subcommand)
    {
        final EmbulkCommandLineParser.Builder parserBuilder = EmbulkCommandLineParser.builder();

        // TODO: Revisit the width. JLine may help. https://github.com/jline
        parserBuilder
            .setWidth(160)
            .addHelpMessageLine("  Help:")
            .addOptionDefinition(OptionDefinition.defineHelpOption("h", "help", "Print help."))
            .addHelpMessageLine("");

        switch (subcommand) {
        case RUN:
            parserBuilder
                .setMainUsage("embulk run <config.yml>")
                .addHelpMessageLine("  Options:")
                // op.on('-r', '--resume-state PATH', 'Path to a file to write or read resume state') do |path|
                //   options[:resume_state_path] = path
                // end
                .addOptionDefinition(OptionDefinition.defineOptionWithArgument(
                    "r", "resume-state", "PATH", "Path to a file to write or read resume state",
                    new OptionBehavior()
                    {
                        public void behave(final EmbulkCommandLine.Builder commandLineBuilder,
                                           final String argument)
                        {
                            commandLineBuilder.setResumeState(argument);
                        }
                    }))
                // op.on('-o', '--output PATH', '(deprecated)') do |path|
                //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: Run with -o option is deprecated. Please use -c option instead. For example,"
                //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: "
                //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}:   $ embulk run config.yml -c diff.yml"
                //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: "
                //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: This -c option stores only diff of the next configuration."
                //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: The diff will be merged to the original config.yml file."
                //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: "
                //   options[:next_config_output_path] = path
                // end
                .addOptionDefinition(OptionDefinition.defineOptionWithArgument(
                    "o", "output", "PATH", "(deprecated)",
                    new OptionBehavior()
                    {
                        public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                        {
                            final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS Z");
                            final String now = DateTime.now().toString(formatter);
                            errorWriter().println(now + ": Run with -o option is deprecated. Please use -c option instead. For example,");
                            errorWriter().println(now + ": ");
                            errorWriter().println(now + ":   $ embulk run config.yml -c diff.yml");
                            errorWriter().println(now + ": ");
                            errorWriter().println(now + ": This -c option stores only diff of the next configuration.");
                            errorWriter().println(now + ": The diff will be merged to the original config.yml file.");
                            errorWriter().println(now + ": ");
                            commandLineBuilder.setOutput(argument);
                        }
                    }))
                // op.on('-c', '--config-diff PATH', 'Path to a file to read & write the next configuration diff') do |path|
                //   options[:next_config_diff_path] = path
                // end
                .addOptionDefinition(OptionDefinition.defineOptionWithArgument(
                    "c", "config-diff", "PATH", "Path to a file to read & write the next configuration diff",
                    new OptionBehavior()
                    {
                        public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                        {
                            commandLineBuilder.setConfigDiff(argument);
                        }
                    }))
                .setArgumentsRange(1, 1);
            addPluginLoadOptionDefinitions(parserBuilder);
            addOtherOptionDefinitions(parserBuilder);
            break;
        case CLEANUP:
            parserBuilder
                .setMainUsage("embulk cleanup <config.yml>")
                .addHelpMessageLine("  Options:")
                // op.on('-r', '--resume-state PATH', 'Path to a file to cleanup resume state') do |path|
                //   options[:resume_state_path] = path
                // end
                .addOptionDefinition(OptionDefinition.defineOptionWithArgument(
                    "r", "resume-state", "PATH", "Path to a file to cleanup resume state",
                    new OptionBehavior()
                    {
                        public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                        {
                            commandLineBuilder.setResumeState(argument);
                        }
                    }))
                .setArgumentsRange(1, 1);
            addPluginLoadOptionDefinitions(parserBuilder);
            addOtherOptionDefinitions(parserBuilder);
            break;
        case PREVIEW:
            parserBuilder
                .setMainUsage("embulk preview <config.yml>")
                .addHelpMessageLine("  Options:")
                // op.on('-G', '--vertical', "Use vertical output format", TrueClass) do |b|
                //   options[:format] = "vertical"
                // end
                .addOptionDefinition(OptionDefinition.defineOptionWithoutArgument(
                    "G", "vertical", "Use vertical output format",
                    new OptionBehavior()
                    {
                        public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                        {
                            commandLineBuilder.setFormat("vertical");
                        }
                    }))
                .setArgumentsRange(1, 1);
            addPluginLoadOptionDefinitions(parserBuilder);
            addOtherOptionDefinitions(parserBuilder);
            break;
        case GUESS:
            parserBuilder
                .setMainUsage("embulk guess <partial-config.yml>")
                .addHelpMessageLine("  Options:")
                // op.on('-o', '--output PATH', 'Path to a file to write the guessed configuration') do |path|
                //   options[:next_config_output_path] = path
                // end
                .addOptionDefinition(OptionDefinition.defineOptionWithArgument(
                    "o", "output", "PATH", "Path to a file to write the guessed configuration",
                    new OptionBehavior()
                    {
                        public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                        {
                            commandLineBuilder.setOutput(argument);
                        }
                    }))
                // op.on('-g', '--guess NAMES', "Comma-separated list of guess plugin names") do |names|
                //   (options[:system_config][:guess_plugins] ||= []).concat names.split(",")  # TODO
                // end
                .addOptionDefinition(OptionDefinition.defineOptionWithArgument(
                    "g", "guess", "NAMES", "Comma-separated list of guess plugin names",
                    new OptionBehavior()
                    {
                        public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                        {
                            for (final String guess : argument.split(",")) {
                                commandLineBuilder.addSystemConfig("guess_plugins", guess);
                            }
                        }
                    }))
                .setArgumentsRange(1, 1);
            addPluginLoadOptionDefinitions(parserBuilder);
            addOtherOptionDefinitions(parserBuilder);
            break;
        case MKBUNDLE:
            parserBuilder
                .setMainUsage("embulk mkbundle <directory> [--path PATH]")
                .addHelpMessageLine("  Options:")
                // op.on('--path PATH', 'Relative path from <directory> for the location to install gems to (e.g. --path shared/bundle).') do |path|
                //   options[:bundle_path] = path
                // end
                .addOptionDefinition(OptionDefinition.defineOnlyLongOptionWithArgument(
                    "path", "PATH",
                    "Relative path from <directory> for the location to install gems to (e.g. --path shared/bundle).",
                    new OptionBehavior()
                    {
                        public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                        {
                            commandLineBuilder.setBundlePath(argument);
                        }
                    }))
                .addHelpMessageLine("")
                .addHelpMessageLine("  \"mkbundle\" creates a new a plugin bundle directory. You can install")
                .addHelpMessageLine("  plugins (gems) to the directory instead of ~/.embulk.")
                .addHelpMessageLine("")
                .addHelpMessageLine("  See generated <directory>/Gemfile to install plugins to the directory.")
                .addHelpMessageLine("  Use -b, --bundle BUNDLE_DIR option to use it:")
                .addHelpMessageLine("")
                .addHelpMessageLine("    $ embulk mkbundle ./dir                # create bundle directory")
                .addHelpMessageLine("    $ (cd dir && vi Gemfile && embulk bundle)   # update plugin list")
                .addHelpMessageLine("    $ embulk guess -b ./dir ...            # guess using bundled plugins")
                .addHelpMessageLine("    $ embulk run   -b ./dir ...            # run using bundled plugins")
                .setArgumentsRange(1, 1);
            break;
        case NEW:
            parserBuilder
                .setMainUsage("embulk new <category> <name>")
                .addUsage("")
                .addUsage("categories:")
                .addUsage("")
                .addUsage("    ruby-input                 Ruby record input plugin    (like \"mysql\")")
                .addUsage("    ruby-output                Ruby record output plugin   (like \"mysql\")")
                .addUsage("    ruby-filter                Ruby record filter plugin   (like \"add-hostname\")")
                .addUsage("    #ruby-file-input           Ruby file input plugin      (like \"ftp\")          # not implemented yet [#21]")
                .addUsage("    #ruby-file-output          Ruby file output plugin     (like \"ftp\")          # not implemented yet [#22]")
                .addUsage("    ruby-parser                Ruby file parser plugin     (like \"csv\")")
                .addUsage("    ruby-formatter             Ruby file formatter plugin  (like \"csv\")")
                .addUsage("    #ruby-decoder              Ruby file decoder plugin    (like \"gzip\")         # not implemented yet [#31]")
                .addUsage("    #ruby-encoder              Ruby file encoder plugin    (like \"gzip\")         # not implemented yet [#32]")
                .addUsage("    java-input                 Java record input plugin    (like \"mysql\")")
                .addUsage("    java-output                Java record output plugin   (like \"mysql\")")
                .addUsage("    java-filter                Java record filter plugin   (like \"add-hostname\")")
                .addUsage("    java-file-input            Java file input plugin      (like \"ftp\")")
                .addUsage("    java-file-output           Java file output plugin     (like \"ftp\")")
                .addUsage("    java-parser                Java file parser plugin     (like \"csv\")")
                .addUsage("    java-formatter             Java file formatter plugin  (like \"csv\")")
                .addUsage("    java-decoder               Java file decoder plugin    (like \"gzip\")")
                .addUsage("    java-encoder               Java file encoder plugin    (like \"gzip\")")
                .addUsage("")
                .addUsage("examples:")
                .addUsage("    new ruby-output hbase")
                .addUsage("    new ruby-filter int-to-string")
                .setArgumentsRange(2, 2);
            break;
        case MIGRATE:
            parserBuilder
                .setMainUsage("embulk migrate <directory>")
                .setArgumentsRange(1, 1);
            break;
        case SELFUPDATE:
            parserBuilder
                .setMainUsage("embulk selfupdate")
                // op.on('-f', "Skip corruption check", TrueClass) do |b|
                //   options[:force] = true
                // end
                .addOptionDefinition(OptionDefinition.defineOnlyShortOptionWithoutArgument(
                    "f", "Skip corruption check",
                    new OptionBehavior()
                    {
                        public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                        {
                            commandLineBuilder.setForce(true);
                        }
                    }))
                .setArgumentsRange(0, 1);
            break;
        case EXAMPLE:
            parserBuilder
                .setMainUsage("embulk example [directory]")
                .setArgumentsRange(0, 1);
            break;
        default:
            parserBuilder.setMainUsage("[FATAL] Unknown subcommand: " + subcommand);
        }

        return parserBuilder.build();
    }

    private int runSubcommand(final EmbulkSubcommand subcommand,
                              final List<String> subcommandArguments,
                              final EmbulkCommandLine commandLine,
                              final List<String> jrubyOptions)
    {
        switch (subcommand) {
        case EXAMPLE:
            final EmbulkExample embulkExample = new EmbulkExample();
            try {
                embulkExample.createExample(commandLine.getArguments().isEmpty()
                                            ? "embulk-example"
                                            : commandLine.getArguments().get(0));
            }
            catch (IOException ex) {
                ex.printStackTrace(System.err);
                return 1;
            }
            return 0;
        case NEW:
            final String categoryWithLanguage = commandLine.getArguments().get(0);
            final String nameGiven = commandLine.getArguments().get(1);
            try {
                final EmbulkNew embulkNew = new EmbulkNew(categoryWithLanguage, nameGiven, this.embulkVersion);
                embulkNew.newPlugin();
            }
            catch (IOException ex) {
                ex.printStackTrace();
                return 1;
            }
            return 0;
        case MIGRATE:
            final String path = commandLine.getArguments().get(0);
            final EmbulkMigrate embulkMigrate = new EmbulkMigrate();
            try {
                embulkMigrate.migratePlugin(path, this.embulkVersion);
            }
            catch (IOException ex) {
                ex.printStackTrace();
                return 1;
            }
            return 0;
        case SELFUPDATE:
            final String specifiedVersionString;
            if (commandLine.getArguments().isEmpty()) {
                specifiedVersionString = null;
            }
            else {
                specifiedVersionString = commandLine.getArguments().get(0);
            }
            final EmbulkSelfUpdate embulkSelfUpdate = new EmbulkSelfUpdate();
            try {
                embulkSelfUpdate.updateSelf(this.embulkVersion, specifiedVersionString, commandLine.getForce());
            }
            catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
                return 1;
            }
            return 0;
        case BUNDLE:
            if (!subcommandArguments.isEmpty() && subcommandArguments.get(0).equals("new")) {
                if (subcommandArguments.size() != 2) {
                    printGeneralUsage(System.err);
                    System.err.println("");
                    System.err.println("Use `<command> --help` to see description of the commands.");
                    return 1;
                }
                newBundle(subcommandArguments.get(1), null);
                System.err.println("'embulk bundle new' is deprecated. This will be removed in future release. Please use 'embulk mkbundle' instead.");
            }
            else {
                runBundler(subcommandArguments, null);
            }
            return 0;
        case GEM:
            callJRubyGem(subcommandArguments);
            return 0;
        case MKBUNDLE:
            newBundle(commandLine.getArguments().get(0), commandLine.getBundlePath());
            break;
        case EXEC:
            callJRubyExec(subcommandArguments);
            return 127;
        case IRB:
            callJRubyIRB();
            return 0;
        case RUN:
        case CLEANUP:
        case PREVIEW:
        case GUESS:
            // NOTE: When it was in Ruby "require 'embulk'" was required on top for Ruby |Embulk::setup|.
            // Ruby |Embulk::setup| is now replaced with Java |org.embulk.EmbulkSetup.setup|.

            // TODO: Move this to initial JRuby instantiation.
            // reset context class loader set by org.jruby.Main.main to nil. embulk manages
            // multiple classloaders. default classloader should be Plugin.class.getClassloader().
            Thread.currentThread().setContextClassLoader(null);

            // NOTE: When it was in Ruby ""require 'json'" was required.

            // NOTE: $LOAD_PATH and $CLASSPATH are set in |EmbulkSetup|.

            // call |EmbulkSetup.setup| after setup_classpaths to allow users to overwrite
            // embulk classes
            // NOTE: |EmbulkSetup.setup| returns |EmbulkEmbed| while it stores Ruby |Embulk::EmbulkRunner(EmbulkEmbed)|
            // into Ruby |Embulk::Runner|.
            final EmbulkRunner runner = EmbulkSetup.setup(commandLine.getSystemConfig());

            final Path configDiffPath =
                (commandLine.getConfigDiff() == null ? null : Paths.get(commandLine.getConfigDiff()));
            final Path outputPath =
                (commandLine.getOutput() == null ? null : Paths.get(commandLine.getOutput()));
            final Path resumeStatePath =
                (commandLine.getResumeState() == null ? null : Paths.get(commandLine.getResumeState()));

            try {
                switch (subcommand) {
                case GUESS:
                    runner.guess(Paths.get(commandLine.getArguments().get(0)), outputPath);
                    break;
                case PREVIEW:
                    runner.preview(Paths.get(commandLine.getArguments().get(0)), commandLine.getFormat());
                    break;
                case RUN:
                    runner.run(Paths.get(commandLine.getArguments().get(0)),
                               configDiffPath,
                               outputPath,
                               resumeStatePath);
                    break;
                }
            }
            catch (Throwable ex) {
                ex.printStackTrace(System.err);
                System.err.println("");
                System.err.println("Error: " + ex.getMessage());
                return 1;
            }
        }
        return 0;
    }

    private int newBundle(final String pathString, final String bundlePath)
    {
        final Path path = Paths.get(pathString);
        if (Files.exists(path)) {
            System.err.println("'" + pathString + "' already exists.");
            return 1;
        }

        System.out.println("Initializing " + pathString + "...");
        try {
            Files.createDirectories(path);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return 1;
        }
        boolean success = false;
        try {
            // TODO: Rewrite this part in Java.
            final ScriptingContainer localJRubyContainer = createLocalJRubyScriptingContainer();
            localJRubyContainer.runScriptlet("require 'embulk'");
            localJRubyContainer.runScriptlet("require 'fileutils'");
            localJRubyContainer.runScriptlet("require 'rubygems/gem_runner'");

            // copy embulk/data/bundle/ contents
            localJRubyContainer.runScriptlet("require 'embulk/data/package_data'");
            localJRubyContainer.put("__internal_path__", pathString);
            localJRubyContainer.runScriptlet("pkg = Embulk::PackageData.new('bundle', __internal_path__)");
            localJRubyContainer.remove("__internal_path__");
            localJRubyContainer.runScriptlet("%w[Gemfile .ruby-version .bundle/config embulk/input/example.rb embulk/output/example.rb embulk/filter/example.rb].each { |file| pkg.cp(file, file) }");
                                                                                                                                    // run the first bundle-install
            runBundler(Arrays.asList("install", "--path", bundlePath != null ? bundlePath : "."), path);
            success = true;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
            // success = true;
        }
        finally {
            if (!success) {
                try {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>()
                        {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                            {
                                try {
                                    Files.deleteIfExists(file);
                                }
                                catch (IOException ex) {
                                    // Ignore.
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exception)
                            {
                                try {
                                    Files.deleteIfExists(dir);
                                }
                                catch (IOException ex) {
                                    // Ignore.
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                    return 1;
                }
            }
        }
        return 0;
    }

    private void runBundler(final List<String> arguments, final Path path)
    {
        final ScriptingContainer localJRubyContainer = createLocalJRubyScriptingContainer();
        localJRubyContainer.runScriptlet("require 'bundler'");  // bundler is included in embulk-core.jar

        // this hack is necessary to make --help working
        localJRubyContainer.runScriptlet("Bundler.define_singleton_method(:which_orig, Bundler.method(:which))");
        localJRubyContainer.runScriptlet("Bundler.define_singleton_method(:which) { |executable| (executable == 'man' ? false : which_orig(executable)) }");

        localJRubyContainer.runScriptlet("require 'bundler/friendly_errors'");
        localJRubyContainer.runScriptlet("require 'bundler/cli'");

        localJRubyContainer.put("__internal_argv_java__", arguments);
        if (path == null) {
            localJRubyContainer.runScriptlet("Bundler.with_friendly_errors { Bundler::CLI.start(Array.new(__internal_argv_java__), debug: true) }");
        }
        else {
            localJRubyContainer.put("__internal_working_dir__", path.toString());
            localJRubyContainer.runScriptlet("Dir.chdir(__internal_working_dir__) { Bundler.with_friendly_errors { Bundler::CLI.start(__internal_argv__, debug: true) } }");
            localJRubyContainer.remove("__internal_working_dir__");
        }
        localJRubyContainer.remove("__internal_argv_java__");
    }

    private void addPluginLoadOptionDefinitions(final EmbulkCommandLineParser.Builder parserBuilder)
    {
        parserBuilder.addHelpMessageLine("");
        parserBuilder.addHelpMessageLine("  Plugin load options:");
        // op.on('-L', '--load PATH', 'Add a local plugin path') do |plugin_path|
        //   plugin_paths << plugin_path
        // end
        parserBuilder.addOptionDefinition(OptionDefinition.defineOptionWithArgument(
            "L", "load", "PATH", "Add a local plugin path",
            new OptionBehavior()
            {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                {
                    commandLineBuilder.addLoad(argument);
                }
            }));
        // op.on('-I', '--load-path PATH', 'Add ruby script directory path ($LOAD_PATH)') do |load_path|
        //   load_paths << load_path
        // end
        parserBuilder.addOptionDefinition(OptionDefinition.defineOptionWithArgument(
            "I", "load-path", "PATH", "Add ruby script directory path ($LOAD_PATH)",
            new OptionBehavior()
            {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                {
                    commandLineBuilder.addLoadPath(argument);
                }
            }));
        // op.on('-C', '--classpath PATH', "Add java classpath separated by #{classpath_separator} (CLASSPATH)") do |classpath|
        //   classpaths.concat classpath.split(classpath_separator)
        // end
        parserBuilder.addOptionDefinition(OptionDefinition.defineOptionWithArgument(
            "C", "classpath", "PATH", "Add java classpath separated by " + java.io.File.pathSeparator + " (CLASSPATH)",
            new OptionBehavior()
            {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                {
                    final String[] classpaths = argument.split("\\" + java.io.File.pathSeparator);
                    for (final String classpath : classpaths) {
                        commandLineBuilder.addSystemConfig("jruby_classpath", classpath);
                    }
                }
            }));
        // op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory (create one using "embulk mkbundle" command)') do |path|
        //   # only for help message. implemented at lib/embulk/command/embulk_bundle.rb
        // end
        parserBuilder.addOptionDefinition(OptionDefinition.defineOptionWithArgument(
            "b", "bundle", "BUNDLE_DIR", "Path to a Gemfile directory (create one using \"embulk mkbundle\" command)",
            new OptionBehavior()
            {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                {
                    commandLineBuilder.setSystemConfig("jruby_global_bundler_plugin_source_directory", argument);
                }
            }));
    }

    private void addOtherOptionDefinitions(final EmbulkCommandLineParser.Builder parserBuilder)
    {
        parserBuilder.addHelpMessageLine("");
        parserBuilder.addHelpMessageLine("  Other options:");
        // op.on('-l', '--log PATH', 'Output log messages to a file (default: -)') do |path|
        //   options[:system_config][:log_path] = path
        // end
        parserBuilder.addOptionDefinition(OptionDefinition.defineOnlyLongOptionWithArgument(
            "log", "PATH", "Output log messages to a file (default: -)",
            new OptionBehavior()
            {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                {
                    commandLineBuilder.setSystemConfig("log_path", argument);
                }
            }));
        // op.on('-l', '--log-level LEVEL', 'Log level (error, warn, info, debug or trace)') do |level|
        //   options[:system_config][:log_level] = level
        // end
        parserBuilder.addOptionDefinition(OptionDefinition.defineOptionWithArgument(
            "l", "log-level", "LEVEL", "Log level (error, warn, info, debug or trace)",
            new OptionBehavior()
            {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                {
                    commandLineBuilder.setSystemConfig("log_level", argument);
                }
            }));
        // op.on('-X KEY=VALUE', 'Add a performance system config') do |kv|
        //   k, v = kv.split('=', 2)
        //   v ||= "true"
        //   options[:system_config][k] = v
        // end
        parserBuilder.addOptionDefinition(OptionDefinition.defineOnlyShortOptionWithArgument(
            "X", "KEY=VALUE", "Add a performance system config",
            new OptionBehavior()
            {
                public void behave(final EmbulkCommandLine.Builder commandLineBuilder, final String argument)
                        throws EmbulkCommandLineParseException
                {
                    try {
                        final String[] keyValue = argument.split("=", 2);
                        commandLineBuilder.setSystemConfig(keyValue[0], keyValue[1]);
                    }
                    catch (Throwable ex) {
                        throw new EmbulkCommandLineParseException(ex);
                    }
                }
            }));
    }

    private void printGeneralUsage(final PrintStream out)
    {
        out.println("Embulk v" + this.embulkVersion);
        out.println("Usage: embulk [-vm-options] <command> [--options]");
        out.println("Commands:");
        out.println("   mkbundle   <directory>                             # create a new plugin bundle environment.");
        out.println("   bundle     [directory]                             # update a plugin bundle environment.");
        out.println("   run        <config.yml>                            # run a bulk load transaction.");
        out.println("   cleanup    <config.yml>                            # cleanup resume state.");
        out.println("   preview    <config.yml>                            # dry-run the bulk load without output and show preview.");
        out.println("   guess      <partial-config.yml> -o <output.yml>    # guess missing parameters to create a complete configuration file.");
        out.println("   gem        <install | list | help>                 # install a plugin or show installed plugins.");
        out.println("   new        <category> <name>                       # generates new plugin template");
        out.println("   migrate    <path>                                  # modify plugin code to use the latest Embulk plugin API");
        out.println("   example    [path]                                  # creates an example config file and csv file to try embulk.");
        out.println("   selfupdate [version]                               # upgrades embulk to the latest released version or to the specified version.");
        out.println("");
        out.println("VM options:");
        out.println("   -E...                            Run an external script to configure environment variables in JVM");
        out.println("                                    (Operations not just setting envs are not recommended nor guaranteed.");
        out.println("                                     Expect side effects by running your external script at your own risk.)");
        out.println("   -J-O                             Disable JVM optimizations to speed up startup time (enabled by default if command is 'run')");
        out.println("   -J+O                             Enable JVM optimizations to speed up throughput");
        out.println("   -J...                            Set JVM options (use -J-help to see available options)");
        out.println("   -R...                            Set JRuby options (use -R--help to see available options)");
    }

    private void printEmbulkVersionHeader(final PrintStream out)
    {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS Z");
        final String now = DateTime.now().toString(formatter);
        out.println(now + ": Embulk v" + this.embulkVersion);
    }

    private void printEmbulkGeneralNotifications(final PrintStream out)
    {
        out.println("");
        out.println("********************************** INFORMATION **********************************");
        out.println("  Join us! Embulk-announce mailing list is up for IMPORTANT annoucement such as");
        out.println("    compatibility-breaking changes and key feature updates.");
        out.println("  https://groups.google.com/forum/#!forum/embulk-announce");
        out.println("*********************************************************************************");
        out.println("");
    }

    // TODO: Check if it is required to process JRuby options.
    private ScriptingContainer createLocalJRubyScriptingContainer()
    {
        // Not |LocalContextScope.SINGLETON| to narrow down considerations.
        final ScriptingContainer localJRubyContainer =
            new ScriptingContainer(LocalContextScope.SINGLETHREAD, LocalVariableBehavior.PERSISTENT);

        // NOTE: Same done in JRubyScriptingModule.
        // Remember to update |org.embulk.jruby.JRubyScriptingModule| when these environment variables are changed.
        localJRubyContainer.runScriptlet("ENV.delete('BUNDLE_GEMFILE')");
        localJRubyContainer.runScriptlet("ENV['GEM_HOME'] = File.expand_path File.join(Java::java.lang.System.properties['user.home'], '.embulk', Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])");
        localJRubyContainer.runScriptlet("ENV['GEM_PATH'] = ''");

        return localJRubyContainer;
    }

    private void callJRubyGem(final List<String> subcommandArguments)
    {
        final ScriptingContainer localJRubyContainer = createLocalJRubyScriptingContainer();

        localJRubyContainer.runScriptlet("puts ''");
        localJRubyContainer.runScriptlet("puts 'Gem plugin path is: %s' % (ENV.has_key?('GEM_HOME') ? ENV['GEM_HOME'] : '(empty)')");
        localJRubyContainer.runScriptlet("puts ''");

        localJRubyContainer.runScriptlet("require 'rubygems/gem_runner'");
        localJRubyContainer.put("__internal_argv_java__", subcommandArguments);
        localJRubyContainer.runScriptlet("Gem::GemRunner.new.run Array.new(__internal_argv_java__)");
        localJRubyContainer.remove("__internal_argv_java__");
    }

    private void callJRubyExec(final List<String> subcommandArguments)
    {
        final ScriptingContainer localJRubyContainer = createLocalJRubyScriptingContainer();
        localJRubyContainer.put("__internal_argv_java__", subcommandArguments);
        localJRubyContainer.runScriptlet("exec(*Array.new(__internal_argv_java__))");
        localJRubyContainer.remove("__internal_argv_java__");
    }

    private void callJRubyIRB()
    {
        final ScriptingContainer localJRubyContainer = createLocalJRubyScriptingContainer();
        localJRubyContainer.runScriptlet("require 'irb'");
        localJRubyContainer.runScriptlet("IRB.start");
    }

    private final String embulkVersion;
}
