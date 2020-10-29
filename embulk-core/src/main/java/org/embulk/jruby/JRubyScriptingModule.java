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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

            final String jruby = embulkSystemProperties.getProperty("jruby");

            final ArrayList<URL> jrubyUrlsBuilt = new ArrayList<>();
            if (jruby != null && !jruby.isEmpty()) {
                // File.pathSeparator is not available here because the property "jruby" expects a URL-like style:
                // "file:" and "mvn:". It now supports only "file:", though.
                //
                // Semicolons basically do not appear in URLs without quoting except for the "optional fields and values"
                // case described in RFC 1738. We don't need to take care of the "optional fields and values" case here.
                // https://tools.ietf.org/html/rfc1738
                for (final String jarLocator : jruby.split("\\;")) {
                    if (jarLocator.startsWith("file:")) {
                        // TODO: Validate the path more.
                        final URL jarUrl;
                        try {
                            jarUrl = new URL(jarLocator);
                        } catch (final MalformedURLException ex) {
                            throw new JRubyInvalidRuntimeException("Embulk system property \"jruby\" is invalid: " + jruby, ex);
                        }
                        jrubyUrlsBuilt.add(jarUrl);
                    } else {
                        throw new JRubyInvalidRuntimeException("Embulk system property \"jruby\" is invalid: " + jruby);
                    }
                }
            }
            this.jrubyUrls = Collections.unmodifiableList(jrubyUrlsBuilt);

            if (embulkSystemProperties.getProperty("jruby_use_default_embulk_gem_home") != null) {
                // TODO: Log it is no longer used.
            }

            this.initializer = JRubyInitializer.of(injector, LoggerFactory.getLogger("init"), embulkSystemProperties);
        }

        @Override  // from |com.google.inject.Provider|
        public ScriptingContainerDelegate get() throws ProvisionException {
            final JRubyClassLoader jrubyClassLoader;
            try {
                jrubyClassLoader = new JRubyClassLoader(this.jrubyUrls, JRubyScriptingModule.class.getClassLoader());
            } catch (final RuntimeException ex) {
                throw new ProvisionException("Failed to initialize JRubyClassLoader.", ex);
            }

            try {
                jrubyClassLoader.loadClass("org.jruby.Main");
            } catch (final ClassNotFoundException ex) {
                return null;
            }

            try {
                final LazyScriptingContainerDelegate jruby = new LazyScriptingContainerDelegate(
                        jrubyClassLoader,
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

        private final List<URL> jrubyUrls;
        private final boolean useGlobalRubyRuntime;
        private final JRubyInitializer initializer;
    }
}
