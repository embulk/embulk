package org.quickload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quickload.config.ConfigSource;
import org.quickload.exec.PluginExecutors;
import org.quickload.spi.ExecTask;

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
