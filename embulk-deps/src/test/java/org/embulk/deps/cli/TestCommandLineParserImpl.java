package org.embulk.deps.cli;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.embulk.EmbulkVersion;
import org.embulk.cli.Command;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCommandLineParserImpl {
    @Test
    public void testRun() throws Exception {
        final CommandLineParserImpl parser = new CommandLineParserImpl();
        final CommandLineImpl commandLine = parse(
                parser,
                "-R--dev",
                "--log-level", "warn",
                "--log-path", "/var/log/some",
                "-Xjruby=file:///some/jruby.jar",
                "-X", "bar=baz",
                "run",
                "-L", "test",
                "-R", "bar",
                "file3.yml",
                "-I", "example");

        assertEquals(Command.RUN, commandLine.getCommand());

        assertEquals(Arrays.asList("file3.yml"), commandLine.getArguments());

        final Properties actualProperties = commandLine.getCommandLineProperties();
        assertEquals(6, actualProperties.size());
        assertEquals(
                "test" + File.separator + "lib" + File.pathSeparator + "example",
                actualProperties.getProperty("jruby_load_path"));
        assertEquals("warn", actualProperties.getProperty("log_level"));
        assertEquals("/var/log/some", actualProperties.getProperty("log_path"));
        assertEquals("--dev,bar", actualProperties.getProperty("jruby_command_line_options"));
        assertEquals("baz", actualProperties.getProperty("bar"));
        assertEquals("file:///some/jruby.jar", actualProperties.getProperty("jruby"));

        assertEquals("", commandLine.getStdOut());
        assertEquals("", commandLine.getStdErr());
    }

    @Test
    public void testHelp() throws Exception {
        final CommandLineParserImpl parser = new CommandLineParserImpl();
        final CommandLineImpl commandLine = parse(parser, "-h");

        assertEquals(Command.NONE, commandLine.getCommand());

        assertEquals(
                "Usage: embulk [common options] <command> [command options]" + NEWLINE
                + NEWLINE
                + "Commands:" + NEWLINE
                + "   run          Run a bulk load transaction." + NEWLINE
                + "   cleanup      Cleanup resume state." + NEWLINE
                + "   preview      Dry-run a bulk load transaction, and preview it." + NEWLINE
                + "   guess        Guess missing parameters to complete configuration." + NEWLINE
                + "   example      Create example files for a quick trial of Embulk." + NEWLINE
                + "   license      Print out the license notice." + NEWLINE
                + "   selfupdate   Upgrade Embulk to the specified version." + NEWLINE
                + "   gem          Run \"gem\" to install a RubyGem plugin." + NEWLINE
                + "   mkbundle     Create a new plugin bundle environment." + NEWLINE
                + "   bundle       Update a plugin bundle environment." + NEWLINE
                + NEWLINE
                + "Common options:" + NEWLINE
                + "   -h, --help                Print help" + NEWLINE
                + "   -version, --version       Show Embulk version" + NEWLINE
                + "   -l, --log-level LEVEL     Set log level (error, warn, info, debug, trace)" + NEWLINE
                + "       --log-path PATH       Output log messages to a file (default: -)" + NEWLINE
                + "   -X KEY=VALUE              Set Embulk system properties" + NEWLINE
                + "   -R OPTION                 Command-line option for JRuby. (Only '--dev')" + NEWLINE
                + NEWLINE,
                commandLine.getStdOut());
        assertEquals("", commandLine.getStdErr());
    }

    @Test
    public void testRunHelp() throws Exception {
        final CommandLineParserImpl parser = new CommandLineParserImpl();
        final CommandLineImpl commandLine = parse(parser, "-h", "run");

        assertEquals(Command.RUN, commandLine.getCommand());

        assertEquals(
                "Usage: embulk [common options] run [command options] <config.yml>" + NEWLINE
                + NEWLINE
                + "\"embulk run\" runs a bulk load transaction." + NEWLINE
                + NEWLINE
                + "Common options:" + NEWLINE
                + "   -h, --help                  Print help" + NEWLINE
                + "   -version, --version         Show Embulk version" + NEWLINE
                + "   -l, --log-level LEVEL       Set log level (error, warn, info, debug, trace)" + NEWLINE
                + "       --log-path PATH         Output log messages to a file (default: -)" + NEWLINE
                + "   -X KEY=VALUE                Set Embulk system properties" + NEWLINE
                + "   -R OPTION                   Command-line option for JRuby. (Only '--dev')" + NEWLINE
                + NEWLINE
                + "Plugin options:" + NEWLINE
                + "   -L, --load PATH             Add a local plugin path" + NEWLINE
                + "   -I, --load-path PATH        Add Ruby script directory path ($LOAD_PATH)" + NEWLINE
                + "   -C, --classpath PATH        Add $CLASSPATH for JRuby separated by '" + File.pathSeparator + "'" + NEWLINE
                + "   -b, --bundle BUNDLE_DIR     Path to a Gemfile directory" + NEWLINE
                + NEWLINE
                + "Other 'run' options:" + NEWLINE
                + "   -r, --resume-state PATH     Path to a file to write or read resume state" + NEWLINE
                + "   -o, --output PATH           (deprecated)" + NEWLINE
                + "   -c, --config-diff PATH      Path to a file of the next configuration diff" + NEWLINE
                + "   -t, --task-report PATH      Path to a file of task report" + NEWLINE
                + NEWLINE,
                commandLine.getStdOut());
        assertEquals("", commandLine.getStdErr());
    }

    @Test
    public void testVersion() throws Exception {
        final CommandLineParserImpl parser = new CommandLineParserImpl();
        final CommandLineImpl commandLine = parse(parser, "--version");
        assertEquals(Command.NONE, commandLine.getCommand());

        assertEquals("Embulk " + EmbulkVersion.VERSION + NEWLINE, commandLine.getStdOut());
        assertEquals("", commandLine.getStdErr());
    }

    @Test
    public void testInvalidOption() throws Exception {
        final CommandLineParserImpl parser = new CommandLineParserImpl();
        final CommandLineImpl commandLine = parse(parser, "-p");
        assertEquals(Command.NONE, commandLine.getCommand());

        assertEquals(
                "Usage: embulk [common options] <command> [command options]" + NEWLINE
                + NEWLINE
                + "Commands:" + NEWLINE
                + "   run          Run a bulk load transaction." + NEWLINE
                + "   cleanup      Cleanup resume state." + NEWLINE
                + "   preview      Dry-run a bulk load transaction, and preview it." + NEWLINE
                + "   guess        Guess missing parameters to complete configuration." + NEWLINE
                + "   example      Create example files for a quick trial of Embulk." + NEWLINE
                + "   license      Print out the license notice." + NEWLINE
                + "   selfupdate   Upgrade Embulk to the specified version." + NEWLINE
                + "   gem          Run \"gem\" to install a RubyGem plugin." + NEWLINE
                + "   mkbundle     Create a new plugin bundle environment." + NEWLINE
                + "   bundle       Update a plugin bundle environment." + NEWLINE
                + NEWLINE
                + "Common options:" + NEWLINE
                + "   -h, --help                Print help" + NEWLINE
                + "   -version, --version       Show Embulk version" + NEWLINE
                + "   -l, --log-level LEVEL     Set log level (error, warn, info, debug, trace)" + NEWLINE
                + "       --log-path PATH       Output log messages to a file (default: -)" + NEWLINE
                + "   -X KEY=VALUE              Set Embulk system properties" + NEWLINE
                + "   -R OPTION                 Command-line option for JRuby. (Only '--dev')" + NEWLINE
                + NEWLINE,
                commandLine.getStdOut());
        assertEquals("embulk: Unrecognized option: -p" + NEWLINE, commandLine.getStdErr());
    }

    private static CommandLineImpl parse(
            final CommandLineParserImpl parser,
            final String... args)
            throws Exception {
        return parser.parse(Arrays.asList(args), logger);
    }

    private static List<String> toList(final String... element) {
        return Arrays.asList(element);
    }

    private static final String NEWLINE = System.getProperty("line.separator");

    private static final Logger logger = LoggerFactory.getLogger(TestCommandLineParserImpl.class);
}
