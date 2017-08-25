package org.embulk.jruby;

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
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
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
        binder.bind(ScriptingContainer.class).toProvider(ScriptingContainerProvider.class).in(Scopes.SINGLETON);

        Multibinder<PluginSource> multibinder = Multibinder.newSetBinder(binder, PluginSource.class);
        multibinder.addBinding().to(JRubyPluginSource.class);
    }

    private static class ScriptingContainerProvider
            implements ProviderWithDependencies<ScriptingContainer>
    {
        private final Injector injector;
        private final boolean useGlobalRubyRuntime;
        private final String gemHome;
        private final List<String> jrubyClasspath;
        private final List<String> jrubyLoadPath;

        @Inject
        public ScriptingContainerProvider(Injector injector, @ForSystemConfig ConfigSource systemConfig)
        {
            this.injector = injector;

            // use_global_ruby_runtime is valid only when it's guaranteed that just one Injector is
            // instantiated in this JVM.
            this.useGlobalRubyRuntime = systemConfig.get(boolean.class, "use_global_ruby_runtime", false);

            this.gemHome = systemConfig.get(String.class, "gem_home", null);

            // TODO get jruby-home from systemConfig to call jruby.container.setHomeDirectory

            final List jrubyLoadPathNonGeneric = systemConfig.get(List.class, "jruby_load_path", null);
            final ArrayList<String> jrubyLoadPathBuilt = new ArrayList<String>();
            if (jrubyLoadPathNonGeneric != null) {
                for (final Object oneJRubyLoadPath : jrubyLoadPathNonGeneric) {
                    if (oneJRubyLoadPath instanceof String) {
                        jrubyLoadPathBuilt.add((String) oneJRubyLoadPath);
                    }
                    else {
                        // It should happen only in very irregular cases. Okay to create |Logger| every time.
                        Logger logger = injector.getInstance(ILoggerFactory.class).getLogger("init");
                        logger.warn("System config \"jruby_load_path\" contains non-String.");
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
                        // It should happen only in very irregular cases. Okay to create |Logger| every time.
                        Logger logger = injector.getInstance(ILoggerFactory.class).getLogger("init");
                        logger.warn("System config \"jruby_classpath\" contains non-String.");
                        jrubyClasspathBuilt.add(oneJRubyClasspath.toString());
                    }
                }
            }
            this.jrubyClasspath = Collections.unmodifiableList(jrubyClasspathBuilt);
        }

        public ScriptingContainer get()
        {
            LocalContextScope scope = (useGlobalRubyRuntime ? LocalContextScope.SINGLETON : LocalContextScope.SINGLETHREAD);
            ScriptingContainer jruby = new ScriptingContainer(scope);

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

            if (gemHome != null) {
                // Overwrites GEM_HOME and GEM_PATH. GEM_PATH becomes same with GEM_HOME. Therefore
                // with this code, there're no ways to set extra GEM_PATHs in addition to GEM_HOME.
                // Here doesn't modify ENV['GEM_HOME'] so that a JVM process can create multiple
                // JRubyScriptingModule instances. However, because Gem loads ENV['GEM_HOME'] when
                // Gem.clear_paths is called, applications may use unexpected GEM_HOME if clear_path
                // is used.
                jruby.callMethod(
                        jruby.runScriptlet("Gem"),
                        "use_paths", gemHome, gemHome);
            }

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
    }
}
