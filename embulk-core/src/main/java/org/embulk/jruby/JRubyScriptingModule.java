package org.embulk.jruby;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import org.embulk.plugin.PluginSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ModelManager;
import org.embulk.exec.ForSystemConfig;
import org.embulk.spi.BufferAllocator;

public class JRubyScriptingModule
        implements Module
{
    public JRubyScriptingModule(ConfigSource systemConfig)
    {
    }

    @Override
    public void configure(Binder binder)
    {
        binder.bind(ScriptingContainerDelegate.class).toProvider(ScriptingContainerProvider.class).in(Scopes.SINGLETON);

        Multibinder<PluginSource> multibinder = Multibinder.newSetBinder(binder, PluginSource.class);
        multibinder.addBinding().to(JRubyPluginSource.class);
    }

    private static class ScriptingContainerProvider
            implements ProviderWithDependencies<ScriptingContainerDelegate>
    {
        private final Injector injector;
        private final Logger logger;
        private final boolean useGlobalRubyRuntime;
        private final String gemHome;
        private final boolean useDefaultEmbulkGemHome;
        private final List<String> jrubyClasspath;
        private final List<String> jrubyLoadPath;
        private final List<String> jrubyOptions;
        private final String jrubyBundlerPluginSourceDirectory;

        @Inject
        public ScriptingContainerProvider(Injector injector, @ForSystemConfig ConfigSource systemConfig)
        {
            this.injector = injector;
            this.logger = injector.getInstance(ILoggerFactory.class).getLogger("init");

            // use_global_ruby_runtime is valid only when it's guaranteed that just one Injector is
            // instantiated in this JVM.
            this.useGlobalRubyRuntime = systemConfig.get(boolean.class, "use_global_ruby_runtime", false);

            this.gemHome = systemConfig.get(String.class, "gem_home", null);
            this.useDefaultEmbulkGemHome =
                systemConfig.get(String.class, "jruby_use_default_embulk_gem_home", "false").equals("true");

            // TODO get jruby-home from systemConfig to call jruby.container.setHomeDirectory

            final List jrubyLoadPathNonGeneric = systemConfig.get(List.class, "jruby_load_path", null);
            final ArrayList<String> jrubyLoadPathBuilt = new ArrayList<String>();
            if (jrubyLoadPathNonGeneric != null) {
                for (final Object oneJRubyLoadPath : jrubyLoadPathNonGeneric) {
                    if (oneJRubyLoadPath instanceof String) {
                        jrubyLoadPathBuilt.add((String) oneJRubyLoadPath);
                    }
                    else {
                        this.logger.warn("System config \"jruby_load_path\" contains non-String.");
                        jrubyLoadPathBuilt.add(oneJRubyLoadPath.toString());
                    }
                }
            }
            this.jrubyLoadPath = Collections.unmodifiableList(jrubyLoadPathBuilt);

            final List jrubyClasspathNonGeneric = systemConfig.get(List.class, "jruby_classpath", new ArrayList());
            final ArrayList<String> jrubyClasspathBuilt = new ArrayList<String>();
            if (jrubyClasspathNonGeneric != null) {
                for (final Object oneJRubyClasspath : jrubyClasspathNonGeneric) {
                    if (oneJRubyClasspath instanceof String) {
                        jrubyClasspathBuilt.add((String) oneJRubyClasspath);
                    }
                    else {
                        this.logger.warn("System config \"jruby_classpath\" contains non-String.");
                        jrubyClasspathBuilt.add(oneJRubyClasspath.toString());
                    }
                }
            }
            this.jrubyClasspath = Collections.unmodifiableList(jrubyClasspathBuilt);

            final List jrubyOptionsNonGeneric = systemConfig.get(List.class, "jruby_command_line_options", null);
            final ArrayList<String> jrubyOptionsBuilt = new ArrayList<String>();
            if (jrubyOptionsNonGeneric != null) {
                for (final Object oneJRubyOption : jrubyOptionsNonGeneric) {
                    if (oneJRubyOption instanceof String) {
                        jrubyOptionsBuilt.add((String) oneJRubyOption);
                    }
                    else {
                        this.logger.warn("System config \"jruby_command_line_options\" contains non-String.");
                        jrubyOptionsBuilt.add(oneJRubyOption.toString());
                    }
                }
            }
            this.jrubyOptions = Collections.unmodifiableList(jrubyOptionsBuilt);

            this.jrubyBundlerPluginSourceDirectory =
                systemConfig.get(String.class, "jruby_global_bundler_plugin_source_directory", null);
        }

        public ScriptingContainerDelegate get() throws ProvisionException
        {
            final ScriptingContainerDelegate.LocalContextScope scope =
                (useGlobalRubyRuntime
                 ? ScriptingContainerDelegate.LocalContextScope.SINGLETON
                 : ScriptingContainerDelegate.LocalContextScope.SINGLETHREAD);
            final ScriptingContainerDelegate jruby;
            try {
                jruby = ScriptingContainerDelegate.create(
                    JRubyScriptingModule.class.getClassLoader(),
                    scope,
                    ScriptingContainerDelegate.LocalVariableBehavior.PERSISTENT);
            } catch (Exception ex) {
                return null;
            }

            this.setGemVariables(jruby);

            for (final String jrubyOption : this.jrubyOptions) {
                try {
                    jruby.processJRubyOption(jrubyOption);
                }
                catch (ScriptingContainerDelegate.UnrecognizedJRubyOptionException ex) {
                    this.logger.error("The \"-R\" option(s) are not recognized in Embulk: -R" + jrubyOption +
                                      ". Please add your requests at: https://github.com/embulk/embulk/issues/707", ex);
                    throw new RuntimeException(ex);

                }
                catch (ScriptingContainerDelegate.NotWorkingJRubyOptionException ex) {
                    this.logger.warn("The \"-R\" option(s) do not work in Embulk: -R" + jrubyOption + ".", ex);
                }
            }

            setBundlerPluginSourceDirectory(jruby, this.jrubyBundlerPluginSourceDirectory);

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

            // Search embulk/java/bootstrap.rb from a $LOAD_PATH.
            // $LOAD_PATH is set by lib/embulk/command/embulk_run.rb if Embulk starts
            // using embulk-cli but it's not set if Embulk is embedded in an application.
            // Here adds this jar's internal resources to $LOAD_PATH for those applciations.

//            List<String> loadPaths = new ArrayList<String>(jruby.getLoadPaths());
//            String coreJarPath = JRubyScriptingModule.class.getProtectionDomain().getCodeSource().getLocation().getPath();
//            if (!loadPaths.contains(coreJarPath)) {
//                loadPaths.add(coreJarPath);
//            }
//            jruby.setLoadPaths(loadPaths);

            // load embulk.rb
            jruby.runScriptlet("require 'embulk'");

            // jruby searches embulk/java/bootstrap.rb from the beginning of $LOAD_PATH.
            jruby.runScriptlet("require 'embulk/java/bootstrap'");

            // TODO validate Embulk::Java::Injected::Injector doesn't exist? If it already exists,
            //      Injector is created more than once in this JVM although use_global_ruby_runtime
            //      is set to true.

            // set some constants
            jruby.callMethod(
                    jruby.runScriptlet("Embulk::Java::Injected"),
                    "const_set", "Injector", injector);
            jruby.callMethod(
                    jruby.runScriptlet("Embulk::Java::Injected"),
                    "const_set", "ModelManager", injector.getInstance(ModelManager.class));
            jruby.callMethod(
                    jruby.runScriptlet("Embulk::Java::Injected"),
                    "const_set", "BufferAllocator", injector.getInstance(BufferAllocator.class));

            // initialize logger
            jruby.callMethod(
                    jruby.runScriptlet("Embulk"),
                    "logger=",
                        jruby.callMethod(
                            jruby.runScriptlet("Embulk::Logger"),
                            "new", injector.getInstance(ILoggerFactory.class).getLogger("ruby")));

            return jruby;
        }

        public Set<Dependency<?>> getDependencies()
        {
            // get() depends on other modules
            return ImmutableSet.of(
                Dependency.get(Key.get(ModelManager.class)),
                Dependency.get(Key.get(BufferAllocator.class)));
        }

        private void setGemVariables(final ScriptingContainerDelegate jruby)
        {
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

        private void setBundlerPluginSourceDirectory(final ScriptingContainerDelegate jruby, final String directory)
        {
            if (directory != null) {
                // bundler is included in embulk-core.jar
                jruby.runScriptlet("require 'bundler'");

                // TODO: Remove the monkey patch once the issue is fixed on Bundler or JRuby side.
                // @see <a href="https://github.com/bundler/bundler/issues/4565">Bundler::SharedHelpers.clean_load_path does cleanup the default load_path on jruby - Issue #4565 - bundler/bundler</a>
                final String monkeyPatchOnSharedHelpersCleanLoadPath =
                    "begin\n" +
                    "  require 'bundler/shared_helpers'\n" +
                    "  module Bundler\n" +
                    "    module DisableCleanLoadPath\n" +
                    "      def clean_load_path\n" +
                    "        # Do nothing.\n" +
                    "      end\n" +
                    "    end\n" +
                    "    module SharedHelpers\n" +
                    "      def included(bundler)\n" +
                    "        bundler.send :include, DisableCleanLoadPath\n" +
                    "      end\n" +
                    "    end\n" +
                    "  end\n" +
                    "rescue LoadError\n" +
                    "  # Ignore LoadError.\n" +
                    "end\n";
                jruby.runScriptlet(monkeyPatchOnSharedHelpersCleanLoadPath);

                // Bundler.load.setup_environment was called in Bundler 1.10.6.
                jruby.runScriptlet("Bundler::SharedHelpers.set_bundle_environment");

                jruby.runScriptlet("require 'bundler/setup'");
                // since here, `require` may load files of different (newer) embulk versions
                // especially following 'embulk/command/embulk_main'.

                // NOTE: It is intentionally not done by building a Ruby statement string from |directory|.
                // It can cause insecure injections.
                //
                // add bundle directory path to load local plugins at ./embulk
                jruby.put("__internal_bundler_plugin_source_directory__", directory);
                jruby.runScriptlet("$LOAD_PATH << File.expand_path(__internal_bundler_plugin_source_directory__)");
                jruby.remove("__internal_bundler_plugin_source_directory__");
            }
        }

        private String buildDefaultGemPath() throws ProvisionException {
            return this.buildEmbulkHome().resolve("lib").resolve("gems").toString();
        }

        private String buildGemfilePath(final String bundleDirectoryString)
                throws ProvisionException {
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
    }
}
