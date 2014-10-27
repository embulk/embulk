package org.quickload.spi;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.queue.BufferQueue;
import org.quickload.queue.PageQueue;

public class ProcResource
{
    private final Injector injector;
    private final PluginManager pluginManager;

    @Inject
    public ProcResource(
            Injector injector,
            PluginManager pluginManager)
    {
        this.injector = injector;
    }

    public Injector getInjector()
    {
        return injector;
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig)
    {
        return pluginManager.newPlugin(iface, typeConfig);
    }

    public void startPluginThread(PluginThread runner)
    {
        runner.run();  // TODO in a new thread
    }

    public BufferQueue newBufferQueue()
    {
        return null;  // TODO
    }

    public PageQueue newPageQueue()
    {
        return null;  // TODO
    }
}
