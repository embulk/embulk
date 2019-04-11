package org.embulk;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.embulk.config.ConfigSource;

// Use EmbulkEmbed instead. To be removed by v0.10 or earlier.
@Deprecated  // https://github.com/embulk/embulk/issues/932
public class EmbulkService {
    public EmbulkService(final ConfigSource systemConfig) {
        this.systemConfig = systemConfig;
    }

    public Injector initialize() {
        if (this.initialized) {
            throw new IllegalStateException("Already initialized");
        }

        final ArrayList<Module> built = new ArrayList<>();
        for (final Module module : this.standardModuleList(systemConfig)) {
            built.add(module);
        }
        for (final Module module : this.getAdditionalModules(systemConfig)) {
            built.add(module);
        }

        final Iterable<? extends Module> initialModules = Collections.unmodifiableList(built);
        final Iterable<? extends Module> overriddenModules = this.overrideModules(initialModules, systemConfig);

        this.injector = Guice.createInjector(overriddenModules);
        this.initialized = true;

        return this.injector;
    }

    @Deprecated
    public synchronized Injector getInjector() {
        if (this.initialized) {
            return this.injector;
        }
        return this.initialize();
    }

    protected Iterable<? extends Module> getAdditionalModules(final ConfigSource systemConfig) {
        return Collections.unmodifiableList(new ArrayList<Module>());
    }

    protected Iterable<? extends Module> overrideModules(
            final Iterable<? extends Module> modules, final ConfigSource systemConfig) {
        return modules;
    }

    @Deprecated
    static List<Module> standardModuleList(final ConfigSource systemConfig) {
        return EmbulkEmbed.standardModuleList(systemConfig);
    }

    protected Injector injector;

    private final ConfigSource systemConfig;

    private boolean initialized;
}
