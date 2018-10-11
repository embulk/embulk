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
    @Deprecated
    public EmbulkService(final ConfigSource systemConfig) {
        this.systemConfig = systemConfig;
    }

    @Deprecated
    protected Iterable<? extends Module> getAdditionalModules(final ConfigSource systemConfigParam) {
        return Collections.unmodifiableList(new ArrayList<Module>());
    }

    @Deprecated
    protected Iterable<? extends Module> overrideModules(
            final Iterable<? extends Module> modules, final ConfigSource systemConfigParam) {
        return modules;
    }

    @Deprecated
    static List<Module> standardModuleList(final ConfigSource systemConfigParam) {
        return EmbulkEmbed.standardModuleList(systemConfigParam);
    }

    @Deprecated
    public Injector initialize() {
        if (this.initialized) {
            throw new IllegalStateException("Already initialized");
        }

        final ArrayList<Module> modulesBuilt = new ArrayList<>();
        modulesBuilt.addAll(standardModuleList(this.systemConfig));
        for (final Module module : getAdditionalModules(this.systemConfig)) {
            modulesBuilt.add(module);
        }

        final Iterable<? extends Module> modulesBeforeOverride = Collections.unmodifiableList(modulesBuilt);

        this.injector = Guice.createInjector(overrideModules(modulesBeforeOverride, this.systemConfig));
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

    private final ConfigSource systemConfig;

    protected Injector injector;
    private boolean initialized;
}
