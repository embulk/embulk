package org.quickload;

import org.quickload.config.ConfigSource;
import org.quickload.exec.ExecModule;
import org.quickload.exec.SystemConfigModule;
import org.quickload.jruby.JRubyScriptingModule;
import org.quickload.plugin.BuiltinPluginSourceModule;

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
