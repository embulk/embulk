package org.embulk.jruby;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.io.File;
import org.slf4j.ILoggerFactory;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import org.jruby.CompatVersion;
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

        @Inject
        public ScriptingContainerProvider(Injector injector, @ForSystemConfig ConfigSource systemConfig)
        {
            this.injector = injector;

            // use_global_ruby_runtime is valid only when it's guaranteed that just one Injector is
            // instantiated in this JVM.
            this.useGlobalRubyRuntime = systemConfig.get(boolean.class, "use_global_ruby_runtime", false);

            // TODO get jruby-home from systemConfig to call jruby.container.setHomeDirectory
            // TODO get jruby-load-paths from systemConfig to call jruby.container.setLoadPaths
        }

        public ScriptingContainer get()
        {
            LocalContextScope scope = (useGlobalRubyRuntime ? LocalContextScope.SINGLETON : LocalContextScope.SINGLETHREAD);
            ScriptingContainer jruby = new ScriptingContainer(scope);
            jruby.setCompatVersion(CompatVersion.RUBY1_9);

            // Search embulk/java/bootstrap.rb from a $LOAD_PATH.
            // $LOAD_PATH is set by lib/embulk/command/embulk.rb if Embulk starts
            // using embulk-cli but it's not set if Embulk is embedded in an application.
            // Here adds this jar's internal resources to $LOAD_PATH for those applciations.

            List<String> loadPaths = new ArrayList<String>(jruby.getLoadPaths());
            String coreJarPath = JRubyScriptingModule.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            if (!loadPaths.contains(coreJarPath)) {
                loadPaths.add(coreJarPath);
            }
            jruby.setLoadPaths(loadPaths);

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

            // load embulk.rb
            jruby.runScriptlet("require 'embulk'");

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
