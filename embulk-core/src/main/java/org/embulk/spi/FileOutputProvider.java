package org.embulk.spi;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

public abstract class FileOutputProvider
        implements Provider<OutputPlugin>
{
    private final Injector injector;

    @Inject
    public FileOutputProvider(Injector injector)
    {
        this.injector = injector;
    }

    public abstract Class<? extends FileOutputPlugin> getFileOutputPluginClass();

    @Override
    public OutputPlugin get()
    {
        return new FileOutputRunner(injector.getInstance(getFileOutputPluginClass()));
    }
}
