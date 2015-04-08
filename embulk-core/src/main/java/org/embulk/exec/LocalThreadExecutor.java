package org.embulk.exec;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import org.embulk.config.ConfigSource;

/*
 * Injected in SINGLETON scope at ExecModule
 */
public class LocalThreadExecutor
{
    private final ExecutorService executor;

    @Inject
    public LocalThreadExecutor(@ForSystemConfig ConfigSource systemConfig)
    {
        int defaultMaxThreads = Runtime.getRuntime().availableProcessors() * 2;
        int maxThreads = systemConfig.get(Integer.class, "max_threads", defaultMaxThreads);
        this.executor = Executors.newFixedThreadPool(maxThreads,
                new ThreadFactoryBuilder()
                        .setNameFormat("embulk-executor-%d")
                        .setDaemon(true)
                        .build());
    }

    public ExecutorService getExecutorService()
    {
        return executor;
    }

    // TODO shutdown
}
