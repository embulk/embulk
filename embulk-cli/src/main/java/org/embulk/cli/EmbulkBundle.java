package org.embulk.cli;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.embulk.EmbulkVersion;
import org.jruby.embed.ScriptingContainer;

public class EmbulkBundle
{
    public static void checkBundle(
            final String[] embulkArgs,
            final List<String> jrubyOptions)
    {
        checkBundleWithEmbulkVersion(embulkArgs, jrubyOptions, EmbulkVersion.VERSION);
    }

    // It accepts |embulkVersion| so that it can receive Embulk's version from Ruby (bin/embulk).
    public static void checkBundleWithEmbulkVersion(
            final String[] embulkArgs,
            final List<String> jrubyOptions,
            final String embulkVersion)
    {
        final String bundlePath = System.getenv("EMBULK_BUNDLE_PATH");

        final ScriptingContainer globalJRubyContainer =
            EmbulkGlobalJRubyScriptingContainer.setup(embulkArgs, jrubyOptions, bundlePath, System.err);

        // NOTE: It was written in Ruby as follows in case |bundlePath| != null:
        //   begin
        //     require 'embulk/command/embulk_main'
        //   rescue LoadError
        //     $LOAD_PATH << File.expand_path('../../', File.dirname(__FILE__))
        //     require 'embulk/command/embulk_main'
        //   end
        //
        // NOTE: It was written in Ruby as follows in case |bundlePath| == null::
        //   require 'embulk/command/embulk_main'
        //
        // TODO: Consider handling LoadError or similar errors.
        final EmbulkRun runner = new EmbulkRun(embulkVersion, globalJRubyContainer);
        runner.run(Arrays.asList(embulkArgs), jrubyOptions);
    }
}
