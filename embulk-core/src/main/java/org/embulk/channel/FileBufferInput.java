package org.embulk.channel;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.embulk.buffer.Buffer;

public class FileBufferInput
        extends BufferInput
{
    Buffer element;

    protected FileBufferInput(DataChannel<Buffer> channel)
    {
        super(channel);
        this.element = FileBufferChannel.END_OF_FILE;
    }

    public boolean nextFile()
    {
        if (element != FileBufferChannel.END_OF_FILE) {
            throw new IllegalStateException("nextFile is called but file is not end yet: " + element);
        }
        element = channel.poll();
        return element != null;
    }

    private class FileIte
            implements Iterator<Buffer>
    {
        @Override
        public boolean hasNext()
        {
            if (element != null) {
                if (element == FileBufferChannel.END_OF_FILE) {
                    return false;
                }
                return true;
            } else {
                element = channel.poll();
                return element != null && element != FileBufferChannel.END_OF_FILE;
            }
        }

        @Override
        public Buffer next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Buffer e = element;
            element = null;
            return e;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<Buffer> iterator()
    {
        return new FileIte();
    }
}
