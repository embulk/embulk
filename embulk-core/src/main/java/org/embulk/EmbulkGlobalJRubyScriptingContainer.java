package org.embulk;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.jruby.util.cli.Options;

/**
 * EmbulkGlobalJRubyScriptingContainer creates a ScriptingContainer instance for global use in Embulk.
 *
 * The creator method is static because the target instance is singleton by definition.
 */
public class EmbulkGlobalJRubyScriptingContainer
{
    private EmbulkGlobalJRubyScriptingContainer()
    {
        // Do not instantiate.
    }

    /**
     * Sets up a ScriptingContainer instance for global use in Embulk.
     */
    public static ScriptingContainer setup(final List<String> jrubyOptions, final PrintStream warning)
    {
        // The JRuby instance is a global singleton so that the settings here affects later execution.
        // The local variable should be persistent so that local variables are set through ScriptingContainer.put.
        final ScriptingContainer jrubyGlobalContainer =
            new ScriptingContainer(LocalContextScope.SINGLETON, LocalVariableBehavior.PERSISTENT);
        final RubyInstanceConfig jrubyGlobalConfig = jrubyGlobalContainer.getProvider().getRubyInstanceConfig();

        for (final String jrubyOption : jrubyOptions) {
            try {
                processJRubyOption(jrubyOption, jrubyGlobalConfig);
            }
            catch (UnrecognizedJRubyOptionException ex) {
                warning.println("[WARN] The \"-R\" option(s) are not recognized in Embulk: -R" + jrubyOption);
                warning.println("[WARN] Please add your requests at: https://github.com/embulk/embulk/issues/707");
                warning.println("");
            }
            catch (NotWorkingJRubyOptionException ex) {
                warning.println("[WARN] The \"-R\" option(s) do not work in Embulk: -R" + jrubyOption);
                warning.println("");
            }
        }

        return jrubyGlobalContainer;
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
