package org.embulk.jruby;

import com.google.inject.Injector;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.embulk.EmbulkSystemProperties;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecInternal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JRubyInitializer {
    private JRubyInitializer(
            final EmbulkSystemProperties embulkSystemProperties,
            final boolean isEmbulkSpecific,
            final boolean initializesGem,
            final Injector injector,
            final Logger logger,
            final String gemHome,
            final String gemPath,
            final List<String> jrubyLoadPath,
            final List<String> jrubyClasspath,
            final List<String> jrubyOptions,
            final String jrubyBundlerPluginSourceDirectory,
            final boolean requiresSigdump) {
        this.embulkSystemProperties = embulkSystemProperties;
        this.isEmbulkSpecific = isEmbulkSpecific;
        this.initializesGem = initializesGem;
        this.injector = injector;
        this.logger = logger;
        this.gemHome = gemHome;
        this.gemPath = gemPath;
        this.jrubyLoadPath = jrubyLoadPath;
        this.jrubyClasspath = jrubyClasspath;
        this.jrubyOptions = jrubyOptions;
        this.jrubyBundlerPluginSourceDirectory = jrubyBundlerPluginSourceDirectory;
        this.requiresSigdump = requiresSigdump;
    }

    static JRubyInitializer of(
            final boolean isEmbulkSpecific,
            final boolean initializesGem,
            final Injector injector,
            final Logger logger,
            final EmbulkSystemProperties embulkSystemProperties) {
        final String gemHome = embulkSystemProperties.getProperty("gem_home", null);
        final String gemPath = embulkSystemProperties.getProperty("gem_path", null);

        // TODO get jruby-home from embulkSystemProperties to call jruby.container.setHomeDirectory
        final String jrubyLoadPathWithPathSeparator = embulkSystemProperties.getProperty("jruby_load_path", null);
        final String jrubyClasspathWithPathSeparator = embulkSystemProperties.getProperty("jruby_classpath", null);
        final String jrubyOptionsSeparatedByComma = embulkSystemProperties.getProperty("jruby_command_line_options", null);
        final String jrubyBundlerPluginSourceDirectory =
                embulkSystemProperties.getProperty("jruby_global_bundler_plugin_source_directory", null);
        final boolean requiresSigdump = embulkSystemProperties.getPropertyAsBoolean("jruby.require.sigdump", false);

        final ArrayList<String> jrubyLoadPathBuilt = new ArrayList<String>();
        if (jrubyLoadPathWithPathSeparator != null) {
            for (final String oneJRubyLoadPath : jrubyLoadPathWithPathSeparator.split("\\" + java.io.File.pathSeparator)) {
                jrubyLoadPathBuilt.add((String) oneJRubyLoadPath);
            }
        }

        final ArrayList<String> jrubyClasspathBuilt = new ArrayList<String>();
        if (jrubyClasspathWithPathSeparator != null) {
            for (final String oneJRubyClasspath : jrubyClasspathWithPathSeparator.split("\\" + java.io.File.pathSeparator)) {
                jrubyClasspathBuilt.add((String) oneJRubyClasspath);
            }
        }

        final ArrayList<String> jrubyOptionsBuilt = new ArrayList<String>();
        if (jrubyOptionsSeparatedByComma != null) {
            for (final String oneJRubyOption : jrubyOptionsSeparatedByComma.split(",")) {
                jrubyOptionsBuilt.add((String) oneJRubyOption);
            }
        }

        return new JRubyInitializer(
                embulkSystemProperties,
                isEmbulkSpecific,
                initializesGem,
                injector,
                logger,
                gemHome,
                gemPath,
                Collections.unmodifiableList(jrubyLoadPathBuilt),
                Collections.unmodifiableList(jrubyClasspathBuilt),
                Collections.unmodifiableList(jrubyOptionsBuilt),
                jrubyBundlerPluginSourceDirectory,
                requiresSigdump);
    }

    void initialize(final ScriptingContainerDelegate jruby) {
        try {
            // JRuby runtime options are processed at first.
            for (final String jrubyOption : this.jrubyOptions) {
                try {
                    jruby.processJRubyOption(jrubyOption);
                } catch (ScriptingContainerDelegate.UnrecognizedJRubyOptionException ex) {
                    this.logger.error("The \"-R\" option(s) are not recognized in Embulk: -R" + jrubyOption
                                      + ". Please add your requests at: https://github.com/embulk/embulk/issues/707",
                                      ex);
                    throw new RuntimeException(ex);
                } catch (ScriptingContainerDelegate.NotWorkingJRubyOptionException ex) {
                    this.logger.warn("The \"-R\" option(s) do not work in Embulk: -R" + jrubyOption + ".", ex);
                }
            }

            // Gem paths and Bundler are configured earlier.
            if (this.initializesGem) {
                this.setGemVariables(jruby);
                this.setBundlerPluginSourceDirectory(jruby, this.jrubyBundlerPluginSourceDirectory);
            }

            // $LOAD_PATH and $CLASSPATH are configured after Gem paths and Bundler.
            for (final String oneJRubyLoadPath : this.jrubyLoadPath) {
                // ruby script directory (use unshift to make it highest priority)
                jruby.put("__internal_load_path__", oneJRubyLoadPath);
                // TODO: Check if $LOAD_PATH already contains it.
                jruby.runScriptlet("$LOAD_PATH.unshift File.expand_path(__internal_load_path__)");
                jruby.remove("__internal_load_path__");
            }
            for (final String oneJRubyClasspath : this.jrubyClasspath) {
                jruby.put("__internal_classpath__", oneJRubyClasspath);
                // $CLASSPATH object doesn't have concat method
                // TODO: Check if $CLASSPATH already contains it.
                jruby.runScriptlet("$CLASSPATH << __internal_classpath__");
                jruby.remove("__internal_classpath__");
            }

            if (this.isEmbulkSpecific) {
                // Loading 'embulk/version' triggers a version check with embulk.gem v0.10.25 or later.
                // (Even in Embulk v0.10.24 or earlier, 'embulk/version' is loaded soon below from 'embulk/logger'.)
                jruby.runScriptlet("require 'embulk/version'");

                // Embulk's base Ruby code is loaded at last.
                jruby.runScriptlet("require 'embulk/logger'");
                jruby.runScriptlet("require 'embulk/java/bootstrap'");

                if (this.requiresSigdump) {
                    try {
                        jruby.runScriptlet("require 'sigdump/setup'");
                    } catch (final RuntimeException ex) {
                        if ("org.jruby.embed.EvalFailedException".equals(ex.getClass().getCanonicalName())) {
                            this.logger.warn("Failed to load 'sigdump' gem into JRuby. " + ex.getMessage());
                        } else {
                            this.logger.error("Failed unexpectedly to load 'sigdump' gem into JRuby.", ex);
                        }
                    }
                }

                final Object java = jruby.runScriptlet("Embulk::Java");
                jruby.callMethod(java, "const_set", "BufferAllocator", Exec.getBufferAllocator());
                jruby.callMethod(java, "const_set", "EmbulkSystemProperties", this.embulkSystemProperties);

                final Object injected = jruby.runScriptlet("Embulk::Java::Injected");
                jruby.callMethod(injected, "const_set", "Injector", injector);
                constSetModelManager(jruby, injected, injector);
                jruby.callMethod(injected, "const_set", "BufferAllocator", Exec.getBufferAllocator());

                jruby.callMethod(jruby.runScriptlet("Embulk"), "logger=", jruby.callMethod(
                                     jruby.runScriptlet("Embulk::Logger"),
                                     "new",
                                     LoggerFactory.getLogger("ruby")));
            }
        } catch (final Throwable ex) {
            throw new JRubyRuntimeException("Failed in initializing JRuby runtime.", ex);
        }
    }

    // TODO: Remove these probing methods, and test through mocked ScriptingContainerDelegate.

    String probeGemHomeForTesting() {
        return this.gemHome;
    }

    String probeGemPathForTesting() {
        return this.gemPath;
    }

    List<String> probeJRubyLoadPathForTesting() {
        return this.jrubyLoadPath;
    }

    List<String> probeJRubyClasspathForTesting() {
        return this.jrubyClasspath;
    }

    List<String> probeJRubyOptionsForTesting() {
        return this.jrubyOptions;
    }

    String probeJRubyBundlerPluginSourceDirectoryForTesting() {
        return this.jrubyBundlerPluginSourceDirectory;
    }

    private void setGemVariables(final ScriptingContainerDelegate jruby) {
        final boolean hasBundleGemfile = jruby.isBundleGemfileDefined();
        if (hasBundleGemfile) {
            this.logger.warn("BUNDLE_GEMFILE has already been set: \"" + jruby.getBundleGemfile() + "\"");
        }

        // Here, it does not modify ENV['GEM_HOME'] and ENV['GEM_PATH']. However, note that
        // ENV['GEM_HOME'] and ENV['GEM_PATH'] can be used unexpectedly when Gem.clear_paths
        // is called unexpectedly. Gem loads ENV['GEM_HOME'] when Gem.clear_paths is called.

        if (this.jrubyBundlerPluginSourceDirectory != null) {
            final String gemfilePath = this.buildGemfilePath(this.jrubyBundlerPluginSourceDirectory);
            if (hasBundleGemfile) {
                this.logger.warn("BUNDLE_GEMFILE is being overwritten: \"" + gemfilePath + "\"");
            } else {
                this.logger.info("BUNDLE_GEMFILE is being set: \"" + gemfilePath + "\"");
            }
            jruby.setBundleGemfile(gemfilePath);
            this.logger.info("Gem's home and path are being cleared.");
            jruby.clearGemPaths();
            this.logger.debug("Gem.paths.home = \"" + jruby.getGemHome() + "\"");
            this.logger.debug("Gem.paths.path = " + jruby.getGemPathInString() + "");
        } else {
            if (hasBundleGemfile) {
                this.logger.warn("BUNDLE_GEMFILE is being unset.");
                jruby.unsetBundleGemfile();
            }
            if (this.gemHome != null) {
                // It expects the Embulk system property "gem_home" is set from org.embulk.cli.EmbulkSystemPropertiesBuilder.
                //
                // The system config "gem_home" is always prioritized.
                this.logger.info("Gem's home and path are set by system configs \"gem_home\": \"" + this.gemHome + "\", \"gem_path\": \"" + this.gemPath + "\"");
                jruby.setGemPaths(this.gemHome, this.gemPath);
                this.logger.debug("Gem.paths.home = \"" + jruby.getGemHome() + "\"");
                this.logger.debug("Gem.paths.path = " + jruby.getGemPathInString() + "");
            } else {
                this.logger.info("Gem's home and path are not managed.");
                this.logger.info("Gem.paths.home = \"" + jruby.getGemHome() + "\"");
                this.logger.info("Gem.paths.path = " + jruby.getGemPathInString() + "");
            }
        }
    }

    private void setBundlerPluginSourceDirectory(final ScriptingContainerDelegate jruby, final String directory) {
        if (directory != null) {
            jruby.runScriptlet("require 'bundler'");

            // TODO: Remove the monkey patch once the issue is fixed on Bundler or JRuby side.
            // @see <a href="https://github.com/bundler/bundler/issues/4565">Bundler::SharedHelpers.clean_load_path does cleanup the default load_path on jruby - Issue #4565 - bundler/bundler</a>
            final String monkeyPatchOnSharedHelpersCleanLoadPath =
                    "begin\n"
                    + "  require 'bundler/shared_helpers'\n"
                    + "  module Bundler\n"
                    + "    module DisableCleanLoadPath\n"
                    + "      def clean_load_path\n"
                    + "        # Do nothing.\n"
                    + "      end\n"
                    + "    end\n"
                    + "    module SharedHelpers\n"
                    + "      def included(bundler)\n"
                    + "        bundler.send :include, DisableCleanLoadPath\n"
                    + "      end\n"
                    + "    end\n"
                    + "  end\n"
                    + "rescue LoadError\n"
                    + "  # Ignore LoadError.\n"
                    + "end\n";
            jruby.runScriptlet(monkeyPatchOnSharedHelpersCleanLoadPath);

            jruby.runScriptlet("require 'bundler/setup'");
        }
    }

    private String buildGemfilePath(final String bundleDirectoryString) {
        final Path bundleDirectory;
        try {
            bundleDirectory = Paths.get(bundleDirectoryString);
        } catch (InvalidPathException ex) {
            throw new JRubyRuntimeException("Bundle directory is invalid: \"" + bundleDirectoryString + "\"", ex);
        }
        return bundleDirectory.toAbsolutePath().resolve("Gemfile").toString();
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    private static void constSetModelManager(
            final ScriptingContainerDelegate jruby,
            final Object injected,
            final Injector injector) {
        jruby.callMethod(injected, "const_set", "ModelManager", ExecInternal.getModelManager());
    }

    private final EmbulkSystemProperties embulkSystemProperties;

    private final boolean isEmbulkSpecific;
    private final boolean initializesGem;
    private final Injector injector;
    private final Logger logger;
    private final String gemHome;
    private final String gemPath;
    private final List<String> jrubyLoadPath;
    private final List<String> jrubyClasspath;
    private final List<String> jrubyOptions;
    private final String jrubyBundlerPluginSourceDirectory;
    private final boolean requiresSigdump;
}
