package org.embulk.jar;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;

import org.slf4j.ILoggerFactory;

import org.embulk.config.ConfigSource;
import org.embulk.plugin.PluginSource;

public class JarPluginSourceModule
        implements Module
{
    public JarPluginSourceModule(ConfigSource systemConfig)
    {
    }

    @Override
    public void configure(Binder binder)
    {
        Multibinder<PluginSource> multibinder = Multibinder.newSetBinder(binder, PluginSource.class);
        multibinder.addBinding().to(JarPluginSource.class);
    }
}
