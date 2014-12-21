package org.embulk.exec;

import com.google.inject.Module;
import com.google.inject.Binder;
import org.embulk.config.ConfigSource;

public class SystemConfigModule
        implements Module
{
    private final ConfigSource systemConfig;

    public SystemConfigModule(ConfigSource systemConfig)
    {
        this.systemConfig = systemConfig;
    }

    @Override
    public void configure(Binder binder)
    {
        binder.bind(ConfigSource.class)
            .annotatedWith(ForSystemConfig.class)
            .toInstance(systemConfig);
    }
}
