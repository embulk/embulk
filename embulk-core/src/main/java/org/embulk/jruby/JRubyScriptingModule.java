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
            this.injector = injector;
            this.embulkSystemProperties = embulkSystemProperties;
            if (this.embulkSystemProperties.getProperty("jruby_use_default_embulk_gem_home") != null) {
                // TODO: Log it is no longer used.
            }
        }

        @Override  // from |com.google.inject.Provider|
        public ScriptingContainerDelegate get() throws ProvisionException {
            try {
                return LazyScriptingContainerDelegate.withInjector(
                        this.injector, LoggerFactory.getLogger("init"), this.embulkSystemProperties);
            } catch (final ProvisionException ex) {
                throw ex;
            } catch (final RuntimeException ex) {
                throw new ProvisionException(ex.getMessage(), ex);
            }
        }

        @Override  // from |com.google.inject.spi.HasDependencies|
        public Set<Dependency<?>> getDependencies() {
            // get() depends on other modules
            final HashSet<Dependency<?>> built = new HashSet<>();
            built.add(Dependency.get(Key.get(BufferAllocator.class)));
            return Collections.unmodifiableSet(built);
        }

        private final Injector injector;
        private final EmbulkSystemProperties embulkSystemProperties;
    }
}
