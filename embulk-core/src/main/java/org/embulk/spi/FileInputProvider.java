package org.embulk.spi;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

public abstract class FileInputProvider
        implements Provider<InputPlugin>
{
    private final Injector injector;

    @Inject
    public FileInputProvider(Injector injector)
    {
        this.injector = injector;
    }

    public abstract Class<? extends FileInputPlugin> getFileInputPluginClass();

    @Override
    public InputPlugin get()
    {
        return new FileInputRunner(injector.getInstance(getFileInputPluginClass()));
    }
}
