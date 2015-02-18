package org.embulk.exec;

public class SetCurrentThreadName
        implements AutoCloseable
{
    private final String original;

    public SetCurrentThreadName(String name)
    {
        this.original = Thread.currentThread().getName();
        Thread.currentThread().setName(name);
    }

    @Override
    public void close()
    {
        Thread.currentThread().setName(original);
    }
}
