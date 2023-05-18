package org.embulk.deps.cli;

import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.embulk.EmbulkVersion;
import org.embulk.cli.Command;
import org.slf4j.Logger;

final class CommandLineImpl extends org.embulk.cli.CommandLine {
    private CommandLineImpl(
            final boolean isValid,
            final Command command,
            final List<String> args,
            final Properties commandLineProperties,
            final String stdout,
            final String stderr) {
        this.isValid = isValid;
        this.command = command;
        this.args = args;
        this.commandLineProperties = commandLineProperties;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    static CommandLineImpl of(final List<String> originalArgs, final Logger logger) {
        final String commandString = findCommandString(originalArgs);
        final Command command = Command.of(commandString);

        switch (command) {
            case NONE:
                return ofNone(originalArgs, logger);
            case UNKNOWN:
                return ofUnknown(originalArgs, commandString, logger);
            case EXEC:
            case IRB:
            case MIGRATE:
            case NEW:
                return ofUnsupported(command, commandString, logger);
            case BUNDLE:
                return ofRubyCommand(command, commandString, originalArgs, BUNDLE_USAGE, BUNDLE_HEADER, logger);
            case GEM:
                return ofRubyCommand(command, commandString, originalArgs, GEM_USAGE, GEM_HEADER, logger);
            case RUN:
                return ofCommand(command, originalArgs, 1, 1, RUN_OPTIONS, RUN_USAGE, RUN_HEADER, logger);
            case CLEANUP:
                return ofCommand(command, originalArgs, 1, 1, CLEANUP_OPTIONS, CLEANUP_USAGE, CLEANUP_HEADER, logger);
            case PREVIEW:
                return ofCommand(command, originalArgs, 1, 1, PREVIEW_OPTIONS, PREVIEW_USAGE, PREVIEW_HEADER, logger);
            case GUESS:
                return ofCommand(command, originalArgs, 1, 1, GUESS_OPTIONS, GUESS_USAGE, GUESS_HEADER, logger);
            case EXAMPLE:
                return ofCommand(command, originalArgs, 0, 1, EXAMPLE_OPTIONS, EXAMPLE_USAGE, EXAMPLE_HEADER, logger);
            case LICENSE:
                return ofCommand(command, originalArgs, 0, 0, LICENSE_OPTIONS, LICENSE_USAGE, LICENSE_HEADER, logger);
            case MKBUNDLE:
                return ofCommand(command, originalArgs, 1, 1, MKBUNDLE_OPTIONS, MKBUNDLE_USAGE, MKBUNDLE_HEADER, logger);
            case SELFUPDATE:
                return ofCommand(command, originalArgs, 1, 1, SELFUPDATE_OPTIONS, SELFUPDATE_USAGE, SELFUPDATE_HEADER, logger);
            default:
                throw new IllegalStateException("'" + commandString + "' is recognized, but unexpectedly unknown.");
        }
    }

    @Override
    public final boolean isValid() {
        return this.isValid;
    }

    @Override
    public final Command getCommand() {
        return this.command;
    }

    @Override
    public final List<String> getArguments() {
        return this.args;
    }

    @Override
    public final Properties getCommandLineProperties() {
        final Properties copied = new Properties();
        for (final String key : this.commandLineProperties.stringPropertyNames()) {
            copied.setProperty(key, this.commandLineProperties.getProperty(key));
        }
        return copied;
    }

    @Override
    public final String getStdOut() {
        return this.stdout;
    }

    @Override
    public final String getStdErr() {
        return this.stderr;
    }

    private static CommandLineImpl ofValid(
            final Command command,
            final String[] args,
            final Properties commandLineProperties,
            final CommandWriters writers) {
        final List<String> argsList = Arrays.asList(args);
        return new CommandLineImpl(
                true,
                command,
                argsList.subList(1, argsList.size()),
                commandLineProperties,
                writers.getStdOut(),
                writers.getStdErr());
    }

    private static CommandLineImpl ofInvalid(final Command command, final CommandWriters writers) {
        return new CommandLineImpl(
                false,
                command,
                Collections.unmodifiableList(new ArrayList<>()),
                new Properties(),
                writers.getStdOut(),
                writers.getStdErr());
    }

    private static CommandLineImpl ofNone(final List<String> originalArgs, final Logger logger) {
        final CommandWriters writers = new CommandWriters();

        final CommandLine commandLine = parse(originalArgs, COMMON_OPTIONS, COMMON_USAGE, COMMANDS_HELP, writers, logger);

        if (commandLine == null) {
            return CommandLineImpl.ofInvalid(Command.NONE, writers);
        } else if (commandLine.hasOption(VERSION.getOpt())) {
            writers.printlnOut("Embulk " + EmbulkVersion.VERSION);
            return CommandLineImpl.ofInvalid(Command.NONE, writers);
        }
        printHelp(COMMON_OPTIONS, COMMON_USAGE, COMMANDS_HELP, writers.getStdOutWriter());
        return CommandLineImpl.ofInvalid(Command.NONE, writers);
    }

    private static CommandLineImpl ofUnknown(final List<String> originalArgs, final String commandString, final Logger logger) {
        final CommandWriters writers = new CommandWriters();

        writers.getStdErrWriter().println("embulk: '" + commandString + "' is not an embulk command.");

        final CommandLine commandLine = parse(originalArgs, COMMON_OPTIONS, COMMON_USAGE, COMMANDS_HELP, writers, logger);

        if (commandLine == null) {
            return CommandLineImpl.ofInvalid(Command.UNKNOWN, writers);
        }
        printHelp(COMMON_OPTIONS, COMMON_USAGE, COMMANDS_HELP, writers.getStdOutWriter());
        return CommandLineImpl.ofInvalid(Command.UNKNOWN, writers);
    }

    private static CommandLineImpl ofUnsupported(final Command command, final String commandString, final Logger logger) {
        final CommandWriters writers = new CommandWriters();

        writers.getStdErrWriter().println("embulk: '" + commandString + "' is no longer available.");

        printHelp(COMMON_OPTIONS, COMMON_USAGE, COMMANDS_HELP, writers.getStdOutWriter());
        return CommandLineImpl.ofInvalid(command, writers);
    }

    private static CommandLineImpl ofCommand(
            final Command command,
            final List<String> originalArgs,
            final int minArgs,
            final int maxArgs,
            final Options options,
            final String usage,
            final String header,
            final Logger logger) {
        final CommandWriters writers = new CommandWriters();
        final CommandLine commandLine = parse(originalArgs, options, usage, header, writers, logger);

        if (commandLine == null) {
            return CommandLineImpl.ofInvalid(command, writers);
        } else if (commandLine.hasOption(HELP.getOpt())) {
            printHelp(options, usage, header, writers.getStdOutWriter());
            return CommandLineImpl.ofInvalid(command, writers);
        }

        final String[] args = commandLine.getArgs();
        if (minArgs > (args.length - 1) || (args.length - 1) > maxArgs) {
            if (minArgs == maxArgs) {
                writers.printlnErr("embulk: '" + command.toString() + "' needs exact " + minArgs + " argument(s).");
            } else {
                writers.printlnErr("embulk: '" + command.toString() + "' needs " + minArgs + " - " + maxArgs + " argument(s).");
            }
            printHelp(options, usage, header, writers.getStdOutWriter());
            return CommandLineImpl.ofInvalid(command, writers);
        }

        final Properties properties = new Properties();
        for (final Option option : commandLine.getOptions()) {
            processOptionToProperties(option, command, properties, logger);
        }

        return CommandLineImpl.ofValid(command, args, properties, writers);
    }

    /**
     * Creates a CommandLineImpl instance for "gem" and "bundle" which pass-thorugh following arguments.
     */
    private static CommandLineImpl ofRubyCommand(
            final Command command,
            final String commandString,
            final List<String> originalArgs,
            final String usage,
            final String header,
            final Logger logger) {
        final int index = originalArgs.indexOf(commandString);
        if (index < 0) {
            throw new IllegalStateException("Internal error: '" + commandString + "' is unexpectedly not found.");
        }

        final CommandWriters writers = new CommandWriters();
        final CommandLine commandLine = parse(originalArgs.subList(0, index + 1), COMMON_OPTIONS, usage, header, writers, logger);

        if (commandLine == null) {
            return CommandLineImpl.ofInvalid(command, writers);
        } else if (commandLine.hasOption(HELP.getOpt())) {
            printHelp(COMMON_OPTIONS, usage, header, writers.getStdOutWriter());
            return CommandLineImpl.ofInvalid(command, writers);
        }

        final String[] args = commandLine.getArgs();
        if (args.length != 1) {
            throw new IllegalStateException("Internal error: Arguments after '" + commandString + "' are unexpectedly processed.");
        }

        final Properties properties = new Properties();
        for (final Option option : commandLine.getOptions()) {
            processOptionToProperties(option, command, properties, logger);
        }

        return new CommandLineImpl(
                true,
                command,
                originalArgs.subList(index + 1, originalArgs.size()),
                properties,
                writers.getStdOut(),
                writers.getStdErr());
    }

    private static CommandLine parse(
            final List<String> originalArgs,
            final Options options,
            final String usage,
            final String header,
            final CommandWriters writers,
            final Logger logger) {
        final DefaultParser parser = new DefaultParser();
        try {
            final CommandLine commandLine = parser.parse(options, originalArgs.toArray(new String[0]), false);
            if (commandLine.hasOption(RUBY.getOpt())) {
                logger.warn("The command line option \"-R\" is deprecated. "
                            + "Set an Embulk system property \"jruby_command_line_options\" instead. "
                            + "\"-X jruby_command_line_options=--dev\", for example.");
            }
            return commandLine;
        } catch (final ParseException ex) {
            writers.printlnErr("embulk: " + ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            printHelp(options, usage, header, writers.getStdOutWriter());
            return null;
        }
    }

    private static void printHelp(final Options options, final String usage, final String header, final PrintWriter writer) {
        final HelpFormatter formatter = new HelpFormatterWithPlaceholders("Usage: ", 22);
        formatter.printHelp(
                writer,
                TERMINAL_WIDTH,
                usage,
                header,
                options,
                3,
                5,
                "",
                false);
        writer.flush();
    }

    /**
     * Finds a command identifier from the original command-line arguments.
     *
     * <p>It pre-uses {@link org.apache.commons.cli.DefaultParser} here so that it can find the right command identifier,
     * for example for: {@code "embulk --log-level debug -X key=value run -I dir test.yml"}
     */
    private static String findCommandString(final List<String> originalArgs) {
        final DefaultParser parser = new DefaultParser();
        final CommandLine commandLine;
        try {
            commandLine = parser.parse(COMMON_OPTIONS, originalArgs.toArray(new String[0]), true);  // stopAtNonOption = true
        } catch (final ParseException ex) {
            return null;  // To be: Command.NONE
        }

        for (final String arg : commandLine.getArgs()) {
            if (!arg.startsWith("-")) {
                return arg;
            }
        }
        return null;
    }

    private static void processOptionToProperties(
            final Option option, final Command command, final Properties properties, final Logger logger) {
        if (LOG_LEVEL.getOpt().equals(option.getOpt())) {
            properties.setProperty("log_level", option.getValue());
        } else if (LOG_PATH.getLongOpt().equals(option.getLongOpt())) {
            properties.setProperty("log_path", option.getValue());
        } else if (PROPERTY.getOpt().equals(option.getOpt())) {
            final String[] values = option.getValues();
            if (values.length == 1) {
                properties.setProperty(values[0], "");
            } else if (values.length == 2) {
                properties.setProperty(values[0], values[1]);
            } else {
                throw new IllegalStateException("'-X' expects key=value, but it unexpectedly had more.");
            }
        } else if (RUBY.getOpt().equals(option.getOpt())) {
            final String oldValue = properties.getProperty("jruby_command_line_options");
            final String newValue = option.getValue();
            if (oldValue == null || oldValue.isEmpty()) {
                properties.setProperty("jruby_command_line_options", newValue);
            } else {
                properties.setProperty("jruby_command_line_options", oldValue + "," + newValue);
            }
        } else if (LOAD.getOpt().equals(option.getOpt())) {  // -L
            final String oldValue = properties.getProperty("jruby_load_path");
            final String newValue = Paths.get(option.getValue()).resolve("lib").toString();
            if (oldValue == null || oldValue.isEmpty()) {
                properties.setProperty("jruby_load_path", newValue);
            } else {
                properties.setProperty("jruby_load_path", oldValue + java.io.File.pathSeparator + newValue);
            }
        } else if (LOAD_PATH.getOpt().equals(option.getOpt())) {  // -I
            final String oldValue = properties.getProperty("jruby_load_path");
            final String newValue = option.getValue();
            if (oldValue == null || oldValue.isEmpty()) {
                properties.setProperty("jruby_load_path", newValue);
            } else {
                properties.setProperty("jruby_load_path", oldValue + java.io.File.pathSeparator + newValue);
            }
        } else if (CLASSPATH.getOpt().equals(option.getOpt())) {
            properties.setProperty("jruby_classpath", option.getValue());
        } else if (BUNDLE.getOpt().equals(option.getOpt())) {
            properties.setProperty("jruby_global_bundler_plugin_source_directory", option.getValue());
        } else if (RESUME_STATE_RUN.getOpt().equals(option.getOpt()) || RESUME_STATE_CLEANUP.getOpt().equals(option.getOpt())) {
            properties.setProperty("resume_state_path", option.getValue());
        } else if (VERTICAL.getOpt().equals(option.getOpt())) {
            properties.setProperty("preview_format", "vertical");
        } else if (OUTPUT.getOpt().equals(option.getOpt()) || OUTPUT_GUESS.getOpt().equals(option.getOpt())) {
            properties.setProperty("output_path", option.getValue());
            if (command == Command.RUN) {
                logger.warn(
                        "Run with -o option is deprecated. Please use -c option instead. For example,\n"
                        + "\n"
                        + "$ embulk run config.yml -c diff.yml\n"
                        + "\n"
                        + "This -c option stores only diff of the next configuration. "
                        + "The diff will be merged to the original config.yml file.");
            }
        } else if (GUESS_PLUGINS.getOpt().equals(option.getOpt())) {
            properties.setProperty("guess_plugins", option.getValue());
        } else if (FORCE_SELFUPDATE.getOpt().equals(option.getOpt())) {
            properties.setProperty("force_selfupdate", "true");
        } else if (BUNDLE_PATH.getLongOpt().equals(option.getLongOpt())) {
            properties.setProperty("bundle_path", option.getValue());
        } else if (CONFIG_DIFF.getOpt().equals(option.getOpt())) {
            properties.setProperty("config_diff_path", option.getValue());
        } else if (TASK_REPORT.getOpt().equals(option.getOpt())) {
            properties.setProperty("task_report_path", option.getValue());
        }
    }

    private static final int TERMINAL_WIDTH = 78;

    private static final String COMMANDS_HELP =
            "\nCommands:\n"
            + "   run          Run a bulk load transaction.\n"
            + "   cleanup      Cleanup resume state.\n"
            + "   preview      Dry-run a bulk load transaction, and preview it.\n"
            + "   guess        Guess missing parameters to complete configuration.\n"
            + "   example      Create example files for a quick trial of Embulk.\n"
            + "   license      Print out the license notice.\n"
            + "   selfupdate   Upgrade Embulk to the specified version.\n"
            + "   gem          Run \"gem\" to install a RubyGem plugin.\n"
            + "   mkbundle     Create a new plugin bundle environment.\n"
            + "   bundle       Update a plugin bundle environment.\n\n";

    static final Option RUBY = Option.builder("R").hasArg().argName("OPTION")
            .desc("Command-line option for JRuby. (Only '--dev')").build();

    static final Option LOG_LEVEL = Option.builder("l").longOpt("log-level").hasArg().argName("LEVEL")
            .desc("Set log level (error, warn, info, debug, trace)").build();

    static final Option LOG_PATH = Option.builder().longOpt("log-path").hasArg().argName("PATH")
            .desc("Output log messages to a file (default: -)").build();

    static final Option PROPERTY = Option.builder("X").hasArgs().numberOfArgs(2).valueSeparator('=').argName("KEY=VALUE")
            .desc("Set Embulk system properties").build();

    static final Option HELP = Option.builder("h").longOpt("help")
            .desc("Print help").build();

    static final Option VERSION = Option.builder("version").longOpt("version")
            .desc("Show Embulk version").build();

    // Plugin load:

    static final Option LOAD = Option.builder("L").longOpt("load").hasArg().argName("PATH")
            .desc("Add a local plugin path").build();

    static final Option LOAD_PATH = Option.builder("I").longOpt("load-path").hasArg().argName("PATH")
            .desc("Add Ruby script directory path ($LOAD_PATH)").build();

    static final Option CLASSPATH = Option.builder("C").longOpt("classpath").hasArg().argName("PATH")
            .desc("Add $CLASSPATH for JRuby separated by '" + java.io.File.pathSeparator + "'").build();

    static final Option BUNDLE = Option.builder("b").longOpt("bundle").hasArg().argName("BUNDLE_DIR")
            .desc("Path to a Gemfile directory").build();

    // Per command:

    static final Option RESUME_STATE_RUN = Option.builder("r").longOpt("resume-state").hasArg().argName("PATH")
            .desc("Path to a file to write or read resume state").build();

    static final Option RESUME_STATE_CLEANUP = Option.builder("r").longOpt("resume-state").hasArg().argName("PATH")
            .desc("Path to a file to cleanup resume state").build();

    static final Option OUTPUT = Option.builder("o").longOpt("output").hasArg().argName("PATH")
            .desc("(deprecated)").build();

    static final Option OUTPUT_GUESS = Option.builder("o").longOpt("output").hasArg().argName("PATH")
            .desc("Path to a file to write the guessed configuration").build();

    static final Option CONFIG_DIFF = Option.builder("c").longOpt("config-diff").hasArg().argName("PATH")
            .desc("Path to a file of the next configuration diff").build();

    static final Option TASK_REPORT = Option.builder("t").longOpt("task-report").hasArg().argName("PATH")
            .desc("Path to a file of task report").build();

    static final Option VERTICAL = Option.builder("G").longOpt("vertical")
            .desc("Use vertical output format").build();

    static final Option GUESS_PLUGINS = Option.builder("g").longOpt("guess").hasArg().argName("NAMES")
            .desc("Comma-separated list of guess plugin names").build();

    static final Option BUNDLE_PATH = Option.builder().longOpt("path").hasArg().argName("PATH")
            .desc("Relative path from <directory> for the location to install gems to (e.g. --path shared/bundle).").build();

    static final Option FORCE_SELFUPDATE = Option.builder("f")
            .desc("Skip corruption check").build();

    private static final String COMMON_USAGE = "embulk [common options] <command> [command options]";

    private static final OptionsWithPlaceholders COMMON_OPTIONS = new OptionsWithPlaceholders()
            .addOption(new PlaceholderOption("Common options:"))
            .addOption(HELP)
            .addOption(VERSION)
            .addOption(LOG_LEVEL)
            .addOption(LOG_PATH)
            .addOption(PROPERTY)
            .addOption(RUBY);

    private static final String BUNDLE_USAGE = "embulk [common options] bundle <arguments and options for bundle>";

    private static final String BUNDLE_HEADER =
            "\n"
            + "\"embulk bundle\" runs Ruby's 'bundle' command in its background. "
            + "Arguments are passed through to Ruby's 'bundle' command as-is. "
            + "\"embulk bundle new\" is disabled, though.\n"
            + "\n"
            + "Examples:\n"
            + "   $ embulk bundle       # Update bundled plugin installation.\n"
            + "   $ embulk bundle new   # DOES NOT WORK!\n"
            + "\n";

    private static final String GEM_USAGE = "embulk [common options] gem <arguments and options for gem>";

    private static final String GEM_HEADER =
            "\n"
            + "\"embulk gem\" runs Ruby's 'gem' command in its background. "
            + "Arguments are passed through to Ruby's 'gem' command as-is.\n"
            + "\n"
            + "Examples:\n"
            + "   $ embulk gem install embulk-input-something   # Install a RubyGem plugin.\n"
            + "   $ embulk gem list                             # List installed RubyGem plugins.\n"
            + "   $ embulk gem help                             # Show help of Ruby's 'gem'.\n"
            + "\n";

    private static final OptionsWithPlaceholders PLUGIN_OPTIONS = COMMON_OPTIONS.clone()
            .addOption(new PlaceholderOption(""))
            .addOption(new PlaceholderOption("Plugin options:"))
            .addOption(LOAD)
            .addOption(LOAD_PATH)
            .addOption(CLASSPATH);

    private static final String RUN_USAGE = "embulk [common options] run [command options] <config.yml>";

    private static final OptionsWithPlaceholders RUN_OPTIONS = PLUGIN_OPTIONS.clone()
            .addOption(BUNDLE)
            .addOption(new PlaceholderOption(""))
            .addOption(new PlaceholderOption("Other 'run' options:"))
            .addOption(RESUME_STATE_RUN)
            .addOption(OUTPUT)
            .addOption(CONFIG_DIFF)
            .addOption(TASK_REPORT);

    private static final String RUN_HEADER =
            "\n"
            + "\"embulk run\" runs a bulk load transaction.\n"
            + "\n";

    private static final String CLEANUP_USAGE = "embulk [common options] cleanup [command options] <config.yml>";

    private static final OptionsWithPlaceholders CLEANUP_OPTIONS = PLUGIN_OPTIONS.clone()
            .addOption(new PlaceholderOption(""))
            .addOption(new PlaceholderOption("Another 'cleanup' option:"))
            .addOption(RESUME_STATE_CLEANUP);

    private static final String CLEANUP_HEADER =
            "\n"
            + "\"embulk cleanup\" cleans up resume state.\n"
            + "\n";

    private static final String PREVIEW_USAGE = "embulk [common options] preview [command options] <config.yml>";

    private static final OptionsWithPlaceholders PREVIEW_OPTIONS = PLUGIN_OPTIONS.clone()
            .addOption(new PlaceholderOption(""))
            .addOption(new PlaceholderOption("Another 'preview' option:"))
            .addOption(VERTICAL);

    private static final String PREVIEW_HEADER =
            "\n"
            + "\"embulk preview\" dry-runs a bulk load transaction, and preview it.\n"
            + "\n";

    private static final String GUESS_USAGE = "embulk [common options] guess [command options] <partial-config.yml>";

    private static final OptionsWithPlaceholders GUESS_OPTIONS = PLUGIN_OPTIONS.clone()
            .addOption(new PlaceholderOption(""))
            .addOption(new PlaceholderOption("Other 'guess' options:"))
            .addOption(OUTPUT_GUESS)
            .addOption(GUESS_PLUGINS);

    private static final String GUESS_HEADER =
            "\n"
            + "\"embulk guess\" guesses missing parameters to complete configuration.\n"
            + "\n";

    private static final String EXAMPLE_USAGE = "embulk [common options] example [command options] [directory]";

    private static final OptionsWithPlaceholders EXAMPLE_OPTIONS = COMMON_OPTIONS.clone();

    private static final String EXAMPLE_HEADER =
            "\n"
            + "\"embulk example\" creates example files for a quick trial of Embulk.\n"
            + "\n";

    private static final String LICENSE_USAGE = "embulk [common options] license";

    private static final OptionsWithPlaceholders LICENSE_OPTIONS = COMMON_OPTIONS.clone();

    private static final String LICENSE_HEADER =
            "\n"
            + "\"embulk license\" prints the license notice.\n"
            + "\n";

    private static final String MKBUNDLE_USAGE = "embulk [common options] mkbundle [command options] <directory>";

    private static final String MKBUNDLE_HEADER =
            "\n"
            + "\"embulk mkbundle\" creates a new a \"bundle\" directory with Bundler and \"Gemfile\". "
            + "You can install RubyGem plugins in the directory instead of Embulk home.\n"
            + "\n"
            + "See/edit the generated <directory>/Gemfile to install RubyGem plugins in the bundle directory. "
            + " Use the \"-b\" or \"--bundle\" option to run with plugins in the bundle directory.\n"
            + "\n"
            + "Example:\n"
            + "   $ embulk mkbundle ./dir       # Create a bundle directory.\n"
            + "   $ cd dir                      # Go to the bundle directory.\n"
            + "   (Edit Gemfile)                # Update the plugin list.\n"
            + "   $ embulk bundle               # Update bundled plugin installation.\n"
            + "   $ cd -                        # Go back to your previous directory.\n"
            + "   $ embulk guess -b ./dir ...   # Guess with the bundled plugins.\n"
            + "   $ embulk run   -b ./dir ...   # Run with bundled plugins.\n"
            + "\n";

    private static final OptionsWithPlaceholders MKBUNDLE_OPTIONS = COMMON_OPTIONS.clone()
            .addOption(new PlaceholderOption(""))
            .addOption(new PlaceholderOption("Another 'mkbundle' option:"))
            .addOption(BUNDLE_PATH);

    private static final String SELFUPDATE_USAGE = "embulk [common options] selfupdate [command options] <version>";

    private static final OptionsWithPlaceholders SELFUPDATE_OPTIONS = COMMON_OPTIONS.clone()
            .addOption(new PlaceholderOption(""))
            .addOption(new PlaceholderOption("Another 'selfupdate' option:"))
            .addOption(FORCE_SELFUPDATE);

    private static final String SELFUPDATE_HEADER =
            "\n"
            + "\"embulk selfupdate\" upgrades Embulk to the specified version.\n"
            + "\n";

    private final boolean isValid;
    private final Command command;
    private final List<String> args;
    private final Properties commandLineProperties;
    private final String stdout;
    private final String stderr;
}
