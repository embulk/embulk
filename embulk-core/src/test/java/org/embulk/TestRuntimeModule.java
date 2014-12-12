package org.embulk;

import org.embulk.config.ConfigSource;
import org.embulk.exec.ExecModule;
import org.embulk.exec.SystemConfigModule;
import org.embulk.jruby.JRubyScriptingModule;
import org.embulk.plugin.BuiltinPluginSourceModule;

import com.google.inject.Binder;
import com.google.inject.Module;

public class TestRuntimeModule
        implements Module
{
    private final ConfigSource systemConfig;

    public TestRuntimeModule()
    {
        this(generateDummySystemConfig());
    }

    public TestRuntimeModule(ConfigSource systemConfig)
    {
        this.systemConfig = systemConfig;
    }

    private static ConfigSource generateDummySystemConfig()
    {
        // TODO set some default values
        return new ConfigSource();
    }

    @Override
    public void configure(Binder binder)
    {
        new SystemConfigModule(systemConfig).configure(binder);
        new ExecModule().configure(binder);
        new BuiltinPluginSourceModule().configure(binder);
        new JRubyScriptingModule().configure(binder);
    }
}
