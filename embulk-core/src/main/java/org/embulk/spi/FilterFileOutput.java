package org.embulk.spi;

public abstract class FilterFileOutput
        implements FileOutput
{
    private final FileOutput out;

    public FilterFileOutput(FileOutput out)
    {
        this.out = out;
    }

    public void add(Buffer buffer)
    {
        out.add(buffer);
    }

    public void nextFile()
    {
        out.nextFile();
    }

    public void finish()
    {
        out.finish();
    }

    public void close()
    {
        out.close();
    }
}
