package org.embulk;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.embulk.config.ConfigSource;
import org.embulk.exec.SystemConfigModule;
import org.embulk.exec.ExecModule;
import org.embulk.exec.ExtensionServiceLoaderModule;
import org.embulk.plugin.PluginClassLoaderModule;
import org.embulk.plugin.BuiltinPluginSourceModule;
import org.embulk.jruby.JRubyScriptingModule;

public class EmbulkService
{
    protected final Injector injector;

    public EmbulkService(ConfigSource systemConfig)
    {
        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(new SystemConfigModule(systemConfig));
        modules.add(new ExecModule());
        modules.add(new ExtensionServiceLoaderModule(systemConfig));
        modules.add(new PluginClassLoaderModule(systemConfig));
        modules.add(new BuiltinPluginSourceModule());
        modules.add(new JRubyScriptingModule(systemConfig));
        modules.addAll(getAdditionalModules(systemConfig));
        injector = Guice.createInjector(modules.build());
    }

    protected Iterable<? extends Module> getAdditionalModules(ConfigSource systemConfig)
    {
        return ImmutableList.of();
    }

    public Injector getInjector()
    {
        return injector;
    }
}
