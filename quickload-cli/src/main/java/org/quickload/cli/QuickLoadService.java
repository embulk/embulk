package org.quickload.cli;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.quickload.config.ConfigSource;
import org.quickload.exec.ExecModule;
import org.quickload.exec.ExtensionServiceLoaderModule;
import org.quickload.plugin.BuiltinPluginSourceModule;
import org.quickload.jruby.JRubyScriptingModule;
import org.quickload.standards.StandardPluginModule;

public class QuickLoadService
{
    protected final Injector injector;

    public QuickLoadService(ConfigSource systemConfig)
    {
        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(new ExecModule());
        modules.add(new ExtensionServiceLoaderModule());
        modules.add(new BuiltinPluginSourceModule());
        modules.add(new StandardPluginModule());
        modules.add(new JRubyScriptingModule());
        modules.addAll(getAdditionalModules());
        injector = Guice.createInjector(modules.build());
    }

    protected Iterable<? extends Module> getAdditionalModules()
    {
        return ImmutableList.of();
    }
}
