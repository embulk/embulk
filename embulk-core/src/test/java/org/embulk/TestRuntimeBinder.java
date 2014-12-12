package org.embulk;

import org.embulk.config.ConfigSource;
import org.embulk.exec.PluginExecutors;
import org.embulk.spi.ExecTask;

public class TestRuntimeBinder
        extends GuiceBinder
{
    public TestRuntimeBinder()
    {
        super(new TestRuntimeModule());
    }

    private ConfigSource execConfig = new ConfigSource();

    public TestRuntimeBinder setExecConfig(ConfigSource execConfig)
    {
        this.execConfig = execConfig;
        return this;
    }

    public ConfigSource getExecConfig()
    {
        return execConfig;
    }

    public ExecTask newExecTask()
    {
        return PluginExecutors.newExecTask(getInjector(),
                new ConfigSource().set("exec", execConfig));
    }
}
