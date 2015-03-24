package org.embulk.spi;

import java.util.List;

public class Page
{
    private final Buffer buffer;
    private List<String> stringReferences;

    protected Page(Buffer buffer)
    {
        this.buffer = buffer;
    }

    public static Page allocate(int length)
    {
        return new Page(Buffer.allocate(length));
    }

    public static Page wrap(Buffer buffer)
    {
        return new Page(buffer);
    }

    public Page setStringReferences(List<String> values)
    {
        this.stringReferences = values;
        return this;
    }

    public List<String> getStringReferences()
    {
        // TODO used by mapreduce executor
        return stringReferences;
    }

    public String getStringReference(int index)
    {
        return stringReferences.get(index);
    }

    public void release()
    {
        buffer.release();
    }

    public Buffer buffer()
    {
        return buffer;
    }
}
