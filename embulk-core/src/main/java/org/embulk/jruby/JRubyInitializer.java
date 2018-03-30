package org.embulk.jruby;

import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.embulk.config.ModelManager;
import org.embulk.spi.BufferAllocator;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public final class JRubyInitializer {
    private JRubyInitializer(
            final Injector injector,
            final Logger logger,
            final String gemHome,
            final boolean useDefaultEmbulkGemHome,
            final List<String> jrubyLoadPath,
            final List<String> jrubyClasspath,
            final List<String> jrubyOptions,
            final String jrubyBundlerPluginSourceDirectory) {
        this.injector = injector;
        this.logger = logger;
        this.gemHome = gemHome;
        this.useDefaultEmbulkGemHome = useDefaultEmbulkGemHome;
        this.jrubyLoadPath = jrubyLoadPath;
        this.jrubyClasspath = jrubyClasspath;
        this.jrubyOptions = jrubyOptions;
        this.jrubyBundlerPluginSourceDirectory = jrubyBundlerPluginSourceDirectory;
    }

    public static JRubyInitializer of(
            final Injector injector,
            final Logger logger,
            final String gemHome,
            final boolean useDefaultEmbulkGemHome,
            final List jrubyLoadPathNonGeneric,
            final List jrubyClasspathNonGeneric,
            final List jrubyOptionsNonGeneric,
            final String jrubyBundlerPluginSourceDirectory) {
        final ArrayList<String> jrubyLoadPathBuilt = new ArrayList<String>();
        if (jrubyLoadPathNonGeneric != null) {
            for (final Object oneJRubyLoadPath : jrubyLoadPathNonGeneric) {
                if (oneJRubyLoadPath instanceof String) {
                    jrubyLoadPathBuilt.add((String) oneJRubyLoadPath);
                } else {
                    logger.warn("System config \"jruby_load_path\" contains non-String.");
                    jrubyLoadPathBuilt.add(oneJRubyLoadPath.toString());
                }
            }
        }

        final ArrayList<String> jrubyClasspathBuilt = new ArrayList<String>();
        if (jrubyClasspathNonGeneric != null) {
            for (final Object oneJRubyClasspath : jrubyClasspathNonGeneric) {
                if (oneJRubyClasspath instanceof String) {
                    jrubyClasspathBuilt.add((String) oneJRubyClasspath);
                } else {
                    logger.warn("System config \"jruby_classpath\" contains non-String.");
                    jrubyClasspathBuilt.add(oneJRubyClasspath.toString());
                }
            }
        }

        final ArrayList<String> jrubyOptionsBuilt = new ArrayList<String>();
        if (jrubyOptionsNonGeneric != null) {
            for (final Object oneJRubyOption : jrubyOptionsNonGeneric) {
                if (oneJRubyOption instanceof String) {
                    jrubyOptionsBuilt.add((String) oneJRubyOption);
                } else {
                    logger.warn("System config \"jruby_command_line_options\" contains non-String.");
                    jrubyOptionsBuilt.add(oneJRubyOption.toString());
                }
            }
        }

        return new JRubyInitializer(
                injector,
                logger,
                gemHome,
                useDefaultEmbulkGemHome,
                Collections.unmodifiableList(jrubyLoadPathBuilt),
                Collections.unmodifiableList(jrubyClasspathBuilt),
                Collections.unmodifiableList(jrubyOptionsBuilt),
                jrubyBundlerPluginSourceDirectory);
    }

    public void initialize(final ScriptingContainerDelegate jruby) {
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
        this.setGemVariables(jruby);
        this.setBundlerPluginSourceDirectory(jruby, this.jrubyBundlerPluginSourceDirectory);

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

        // Embulk's base Ruby code is loaded at last.
        jruby.runScriptlet("require 'embulk/logger'");
        jruby.runScriptlet("require 'embulk/java/bootstrap'");

        final Object injected = jruby.runScriptlet("Embulk::Java::Injected");
        jruby.callMethod(injected, "const_set", "Injector", injector);
        jruby.callMethod(injected, "const_set", "ModelManager", injector.getInstance(ModelManager.class));
        jruby.callMethod(injected, "const_set", "BufferAllocator", injector.getInstance(BufferAllocator.class));

        jruby.callMethod(jruby.runScriptlet("Embulk"), "logger=", jruby.callMethod(
                             jruby.runScriptlet("Embulk::Logger"),
                             "new",
                             injector.getInstance(ILoggerFactory.class).getLogger("ruby")));
    }

    // TODO: Remove these probing methods, and test through mocked ScriptingContainerDelegate.

    String probeGemHomeForTesting() {
        return this.gemHome;
    }

    boolean probeUseDefaultEmbulkGemHomeForTesting() {
        return this.useDefaultEmbulkGemHome;
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
                // The system config "gem_home" is always prioritized.
                //
                // Overwrites GEM_HOME and GEM_PATH. GEM_PATH becomes same with GEM_HOME. Therefore
                // with this code, there're no ways to set extra GEM_PATHs in addition to GEM_HOME.
                // Here doesn't modify ENV['GEM_HOME'] so that a JVM process can create multiple
                // JRubyScriptingModule instances. However, because Gem loads ENV['GEM_HOME'] when
                // Gem.clear_paths is called, applications may use unexpected GEM_HOME if clear_path
                // is used.
                this.logger.info("Gem's home and path are set by system config \"gem_home\": \"" + this.gemHome + "\"");
                jruby.setGemPaths(this.gemHome);
                this.logger.debug("Gem.paths.home = \"" + jruby.getGemHome() + "\"");
                this.logger.debug("Gem.paths.path = " + jruby.getGemPathInString() + "");
            } else if (this.useDefaultEmbulkGemHome) {
                // NOTE: Same done in "gem", "exec", and "irb" subcommands.
                // Remember to update |org.embulk.cli.EmbulkRun| as well when these environment variables are change
                final String defaultGemHome = this.buildDefaultGemPath();
                this.logger.info("Gem's home and path are set by default: \"" + defaultGemHome + "\"");
                jruby.setGemPaths(defaultGemHome);
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

    private String buildDefaultGemPath() throws ProvisionException {
        return this.buildEmbulkHome().resolve("lib").resolve("gems").toString();
    }

    private String buildGemfilePath(final String bundleDirectoryString) throws ProvisionException {
        final Path bundleDirectory;
        try {
            bundleDirectory = Paths.get(bundleDirectoryString);
        } catch (InvalidPathException ex) {
            throw new ProvisionException("Bundle directory is invalid: \"" + bundleDirectoryString + "\"", ex);
        }
        return bundleDirectory.toAbsolutePath().resolve("Gemfile").toString();
    }

    private Path buildEmbulkHome() throws ProvisionException {
        final String userHomeProperty = System.getProperty("user.home");

        if (userHomeProperty == null) {
            throw new ProvisionException("User home directory is not set in Java properties.");
        }

        final Path userHome;
        try {
            userHome = Paths.get(userHomeProperty);
        } catch (InvalidPathException ex) {
            throw new ProvisionException("User home directory is invalid: \"" + userHomeProperty + "\"", ex);
        }

        return userHome.toAbsolutePath().resolve(".embulk");
    }

    private final Injector injector;
    private final Logger logger;
    private final String gemHome;
    private final boolean useDefaultEmbulkGemHome;
    private final List<String> jrubyLoadPath;
    private final List<String> jrubyClasspath;
    private final List<String> jrubyOptions;
    private final String jrubyBundlerPluginSourceDirectory;
}
