package org.embulk.spi;

public abstract class FilterFileInput
        implements FileInput
{
    protected final FileInput in;

    public FilterFileInput(FileInput in)
    {
        this.in = in;
    }

    @Override
    public Buffer poll()
    {
        return in.poll();
    }

    @Override
    public boolean nextFile()
    {
        return in.nextFile();
    }

    @Override
    public void close()
    {
        return in.close();
    }
}
