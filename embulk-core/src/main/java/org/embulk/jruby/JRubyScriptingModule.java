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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.embulk.EmbulkSystemProperties;
import org.embulk.spi.BufferAllocator;
import org.slf4j.LoggerFactory;

public class JRubyScriptingModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(ScriptingContainerDelegate.class).toProvider(ScriptingContainerProvider.class).in(Scopes.SINGLETON);
    }

    private static class ScriptingContainerProvider
            implements ProviderWithDependencies<ScriptingContainerDelegate> {
        @Inject
        public ScriptingContainerProvider(final Injector injector, final EmbulkSystemProperties embulkSystemProperties) {
            // use_global_ruby_runtime is valid only when it's guaranteed that just one Injector is
            // instantiated in this JVM.
            this.useGlobalRubyRuntime = embulkSystemProperties.getPropertyAsBoolean("use_global_ruby_runtime", false);

            this.initializer = JRubyInitializer.of(
                    injector,
                    LoggerFactory.getLogger("init"),

                    embulkSystemProperties.getProperty("gem_home", null),
                    embulkSystemProperties.getProperty("gem_path", null),
                    embulkSystemProperties.getPropertyAsBoolean("jruby_use_default_embulk_gem_home", false),

                    // TODO get jruby-home from embulkSystemProperties to call jruby.container.setHomeDirectory
                    embulkSystemProperties.getProperty("jruby_load_path", null),
                    embulkSystemProperties.getProperty("jruby_classpath", null),
                    embulkSystemProperties.getProperty("jruby_command_line_options", null),

                    embulkSystemProperties.getProperty("jruby_global_bundler_plugin_source_directory", null),

                    embulkSystemProperties.getPropertyAsBoolean("jruby.require.sigdump", false));
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
        @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
        public Set<Dependency<?>> getDependencies() {
            // get() depends on other modules
            final HashSet<Dependency<?>> built = new HashSet<>();
            built.add(Dependency.get(Key.get(org.embulk.config.ModelManager.class)));
            built.add(Dependency.get(Key.get(BufferAllocator.class)));
            return Collections.unmodifiableSet(built);
        }

        private final boolean useGlobalRubyRuntime;
        private final JRubyInitializer initializer;
    }
}
