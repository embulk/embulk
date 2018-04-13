package org.embulk.jruby;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.embulk.config.ConfigSource;
import org.embulk.config.ModelManager;
import org.embulk.exec.ForSystemConfig;
import org.embulk.spi.BufferAllocator;
import org.slf4j.ILoggerFactory;

public class JRubyScriptingModule implements Module {
    public JRubyScriptingModule(ConfigSource systemConfig) {}

    @Override
    public void configure(Binder binder) {
        binder.bind(ScriptingContainerDelegate.class).toProvider(ScriptingContainerProvider.class).in(Scopes.SINGLETON);

        // TODO: Bind org.jruby.embed.ScriptingContainer without Java-level reference to the class.
        // TODO: Remove this binding finally. https://github.com/embulk/embulk/issues/1007
        binder.bind(org.jruby.embed.ScriptingContainer.class)
                .toProvider(RawScriptingContainerProvider.class).in(Scopes.SINGLETON);
    }

    private static class ScriptingContainerProvider
            implements ProviderWithDependencies<ScriptingContainerDelegate> {
        @Inject
        public ScriptingContainerProvider(Injector injector, @ForSystemConfig ConfigSource systemConfig) {
            // use_global_ruby_runtime is valid only when it's guaranteed that just one Injector is
            // instantiated in this JVM.
            this.useGlobalRubyRuntime = systemConfig.get(boolean.class, "use_global_ruby_runtime", false);

            this.initializer = JRubyInitializer.of(
                    injector,
                    injector.getInstance(ILoggerFactory.class).getLogger("init"),

                    systemConfig.get(String.class, "gem_home", null),
                    systemConfig.get(String.class, "jruby_use_default_embulk_gem_home", "false").equals("true"),

                    // TODO get jruby-home from systemConfig to call jruby.container.setHomeDirectory
                    systemConfig.get(List.class, "jruby_load_path", null),
                    systemConfig.get(List.class, "jruby_classpath", new ArrayList()),
                    systemConfig.get(List.class, "jruby_command_line_options", null),

                    systemConfig.get(String.class, "jruby_global_bundler_plugin_source_directory", null));
        }

        @Override  // from |com.google.inject.Provider|
        public ScriptingContainerDelegate get() throws ProvisionException {
            try {
                final LazyScriptingContainerDelegate jruby = new LazyScriptingContainerDelegate(
                        JRubyScriptingModule.class.getClassLoader(),
                        this.useGlobalRubyRuntime
                                ? ScriptingContainerDelegate.LocalContextScope.SINGLETON
                                : ScriptingContainerDelegate.LocalContextScope.SINGLETHREAD,
                        ScriptingContainerDelegate.LocalVariableBehavior.PERSISTENT,
                        this.initializer);
                if (this.useGlobalRubyRuntime) {
                    // In case the global JRuby instance is used, the instance should be always initialized.
                    // Ruby tests (src/test/ruby/ of embulk-core and embulk-standards) are examples.
                    jruby.getInitialized();
                }
                return jruby;
            } catch (Exception ex) {
                return null;
            }
        }

        @Override  // from |com.google.inject.spi.HasDependencies|
        public Set<Dependency<?>> getDependencies() {
            // get() depends on other modules
            final HashSet<Dependency<?>> built = new HashSet<>();
            built.add(Dependency.get(Key.get(ModelManager.class)));
            built.add(Dependency.get(Key.get(BufferAllocator.class)));
            return Collections.unmodifiableSet(built);
        }

        private final boolean useGlobalRubyRuntime;
        private final JRubyInitializer initializer;
    }

    // TODO: Remove the Java-level reference to org.jruby.embed.ScriptingContainer.
    // TODO: Remove this inner Provider class finally. https://github.com/embulk/embulk/issues/1007
    private static class RawScriptingContainerProvider
            implements ProviderWithDependencies<org.jruby.embed.ScriptingContainer> {
        @Inject
        public RawScriptingContainerProvider(final Injector injector, final ScriptingContainerDelegate delegate) {
            this.delegate = delegate;
            this.logger = injector.getInstance(ILoggerFactory.class).getLogger("init");
        }

        @Override  // from |com.google.inject.Provider|
        public org.jruby.embed.ScriptingContainer get() throws ProvisionException {
            // TODO: Report this deprecation through a reporter.
            this.logger.warn("DEPRECATION: JRuby org.jruby.embed.ScriptingContainer is directly injected.");
            try {
                return (org.jruby.embed.ScriptingContainer) this.delegate.getScriptingContainer();
            } catch (ClassCastException ex) {
                throw new ProvisionException("Invalid JRuby ScriptingContainer instance.", ex);
            }
        }

        @Override  // from |com.google.inject.spi.HasDependencies|
        public Set<Dependency<?>> getDependencies() {
            // get() depends on other modules
            final HashSet<Dependency<?>> built = new HashSet<>();
            built.add(Dependency.get(Key.get(ScriptingContainerDelegate.class)));
            return Collections.unmodifiableSet(built);
        }

        private final ScriptingContainerDelegate delegate;
        private final org.slf4j.Logger logger;
    }
}
