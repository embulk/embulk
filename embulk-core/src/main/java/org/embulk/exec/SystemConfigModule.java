package org.embulk.exec;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.ConfigSource;

public class SystemConfigModule implements Module {
    @Deprecated  // To be removed. Kept only for providing system config with @ForSystemConfig.
    private final ConfigSource systemConfig;

    private final EmbulkSystemProperties embulkSystemProperties;

    public SystemConfigModule(final ConfigSource systemConfig, final EmbulkSystemProperties embulkSystemProperties) {
        this.systemConfig = systemConfig;
        this.embulkSystemProperties = embulkSystemProperties;
    }

    @SuppressWarnings("deprecation")  // Using ForSystemConfig
    @Override
    public void configure(Binder binder) {
        binder.bind(ConfigSource.class)
                .annotatedWith(ForSystemConfig.class)
                .toInstance(systemConfig);
        binder.bind(EmbulkSystemProperties.class)
                .toInstance(embulkSystemProperties);
    }
}
