package org.embulk;

import java.util.List;
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
import static com.google.common.base.Preconditions.checkState;

@Deprecated
public class EmbulkService
{
    private final ConfigSource systemConfig;

    protected Injector injector;
    private boolean initialized;

    public EmbulkService(ConfigSource systemConfig)
    {
        this.systemConfig = systemConfig;
    }

    protected Iterable<? extends Module> getAdditionalModules(ConfigSource systemConfig)
    {
        return ImmutableList.of();
    }

    protected Iterable<? extends Module> overrideModules(Iterable<? extends Module> modules, ConfigSource systemConfig)
    {
        return modules;
    }

    static List<Module> standardModuleList(ConfigSource systemConfig)
    {
        return ImmutableList.of(
                new SystemConfigModule(systemConfig),
                new ExecModule(),
                new ExtensionServiceLoaderModule(systemConfig),
                new PluginClassLoaderModule(systemConfig),
                new BuiltinPluginSourceModule(),
                new JRubyScriptingModule(systemConfig));
    }

    public Injector initialize()
    {
        checkState(!initialized, "Already initialized");

        ImmutableList.Builder<Module> builder = ImmutableList.builder();
        builder.addAll(standardModuleList(systemConfig));
        builder.addAll(getAdditionalModules(systemConfig));

        Iterable<? extends Module> modules = builder.build();
        modules = overrideModules(modules, systemConfig);

        injector = Guice.createInjector(modules);
        initialized = true;

        return injector;
    }

    @Deprecated
    public synchronized Injector getInjector()
    {
        if (initialized) {
            return injector;
        }
        return initialize();
    }
}
