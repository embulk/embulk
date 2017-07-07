package org.embulk.cli;

import java.util.ArrayList;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jruby.util.cli.Options;

public class Main
{
    public static void main(final String[] args)
    {
        final ArrayList<String> jrubyOptions = new ArrayList<String>();

        int i;
        for (i = 0; i < args.length; ++i) {
            if (args[i].startsWith("-R")) {
                jrubyOptions.add(args[i].substring(2));
            } else {
                break;
            }
        }

        final String[] embulkArgs = new String[args.length - i];
        for (int j = 0; i < args.length; ++i, ++j) {
            embulkArgs[j] = args[i];
        }

        // Running embulk/command/embulk_bundle.rb in CLASSPATH (in the JAR file)
        final ScriptingContainer jrubyGlobalContainer = new ScriptingContainer(LocalContextScope.SINGLETON);
        final RubyInstanceConfig jrubyGlobalConfig = jrubyGlobalContainer.getProvider().getRubyInstanceConfig();

        for (final String jrubyOption : jrubyOptions) {
            try {
                processJRubyOption(jrubyOption, jrubyGlobalConfig);
            }
            catch (UnrecognizedJRubyOptionException ex) {
                System.err.println("[WARN] The \"-R\" option(s) are not recognized in Embulk: -R" + jrubyOption);
                System.err.println("[WARN] Please add your requests at: https://github.com/embulk/embulk/issues/707");
                System.err.println("");
            }
            catch (NotWorkingJRubyOptionException ex) {
                System.err.println("[WARN] The \"-R\" option(s) do not work in Embulk: -R" + jrubyOption);
                System.err.println("");
            }
        }

        jrubyGlobalContainer.setArgv(embulkArgs);
        jrubyGlobalContainer.runScriptlet(PathType.CLASSPATH, "embulk/command/embulk_bundle.rb");
    }

    private static final class UnrecognizedJRubyOptionException extends Exception {}
    private static final class NotWorkingJRubyOptionException extends Exception {}

    private static void processJRubyOption(final String jrubyOption, final RubyInstanceConfig jrubyGlobalConfig)
            throws UnrecognizedJRubyOptionException, NotWorkingJRubyOptionException
    {
        if (jrubyOption.charAt(0) != '-') {
            throw new UnrecognizedJRubyOptionException();
        }

        for (int index = 1; index < jrubyOption.length(); ++index) {
            switch (jrubyOption.charAt(index)) {
            case '-':
                if (jrubyOption.equals("--dev")) {
                    // They are not all of "--dev", but they are most possible configurations after JVM boot.
                    Options.COMPILE_INVOKEDYNAMIC.force("false");  // NOTE: Options is global.
                    jrubyGlobalConfig.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
                    return;
                }
                else if (jrubyOption.equals("--client")) {
                    throw new NotWorkingJRubyOptionException();
                }
                else if (jrubyOption.equals("--server")) {
                    throw new NotWorkingJRubyOptionException();
                }
                throw new UnrecognizedJRubyOptionException();
            default:
                throw new UnrecognizedJRubyOptionException();
            }
        }
    }
}
