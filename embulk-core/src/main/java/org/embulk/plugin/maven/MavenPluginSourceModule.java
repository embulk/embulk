package org.embulk.plugin.maven;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import org.embulk.config.ConfigSource;
import org.embulk.plugin.PluginSource;

public class MavenPluginSourceModule implements Module {
    public MavenPluginSourceModule(ConfigSource systemConfig) {}

    @Override
    public void configure(Binder binder) {
        Multibinder<PluginSource> multibinder = Multibinder.newSetBinder(binder, PluginSource.class);
        multibinder.addBinding().to(MavenPluginSource.class);
    }
}
