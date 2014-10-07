package org.quickload.plugin;

public class SetThreadContextClassLoader
        implements AutoCloseable
{
    private final ClassLoader original;

    public SetThreadContextClassLoader(ClassLoader classLoader)
    {
        this.original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Override
    public void close()
    {
        Thread.currentThread().setContextClassLoader(original);
    }
}
