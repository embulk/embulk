package org.embulk.plugin;

import com.google.inject.Module;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

public class BuiltinPluginSourceModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        Multibinder<PluginSource> multibinder = Multibinder.newSetBinder(binder, PluginSource.class);
        //multibinder.addBinding().to(LocalDirectoryPluginSource.class);  // TODO
        multibinder.addBinding().to(InjectedPluginSource.class);
    }
}
