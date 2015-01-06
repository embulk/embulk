package org.embulk.spi;

import java.util.Iterator;

public class ListFileInput
        implements FileInput
{
    private Iterable<Buffer> source;
    private Iterator<Buffer> iterator;

    public ListFileInput(Iterable<Buffer> buffers)
    {
        this.source = buffers;
    }

    public Buffer poll()
    {
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    public boolean nextFile()
    {
        if (source != null) {
            iterator = source.iterator();
            source = null;
            return true;
        } else {
            return false;
        }
    }

    public void close()
    {
        while (nextFile()) {
            while (true) {
                Buffer b = poll();
                if (b == null) {
                    break;
                }
                b.release();
            }
        }
    }
}
