package org.embulk.spi;

import java.util.List;
import java.util.ArrayList;

public class MockFileOutput
        implements FileOutput
{
    private List<List<Buffer>> files = new ArrayList<List<Buffer>>();
    private List<Buffer> lastBuffers = null;
    private boolean finished = false;
    private boolean closed = false;

    public List<List<Buffer>> getFiles()
    {
        return files;
    }

    public List<Buffer> getLastBuffers()
    {
        return lastBuffers;
    }

    public void nextFile()
    {
        lastBuffers = new ArrayList<Buffer>();
        files.add(lastBuffers);
    }

    public boolean isFinished()
    {
        return finished;
    }

    public boolean isClosed()
    {
        return closed;
    }

    public void add(Buffer buffer)
    {
        if (lastBuffers == null) {
            throw new IllegalStateException("FileOutput.nextFile is not called");
        }
        if (finished) {
            throw new IllegalStateException("FileOutput is already finished");
        }
        if (closed) {
            throw new IllegalStateException("FileOutput is already closed");
        }
        lastBuffers.add(buffer);
    }

    public void finish()
    {
        finished = true;
    }

    public void close()
    {
        closed = true;
    }
}
