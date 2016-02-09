package org.embulk.exec;

import static java.util.Locale.ENGLISH;

public class SetCurrentThreadName
        implements AutoCloseable
{
    private final String original;

    public SetCurrentThreadName(String name)
    {
        this.original = Thread.currentThread().getName();
        Thread thread = Thread.currentThread();
        thread.setName(String.format(ENGLISH, "%04d:", thread.getId()) + name);
    }

    @Override
    public void close()
    {
        Thread.currentThread().setName(original);
    }
}
