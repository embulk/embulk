package org.embulk.spi.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.embulk.spi.Buffer;
import org.embulk.spi.FileInput;

public class ListFileInput
        implements FileInput
{
    private Iterator<? extends Iterable<Buffer>> files;
    private Iterator<Buffer> currentBuffers;

    public ListFileInput(Iterable<? extends Iterable<Buffer>> files)
    {
        this.files = files.iterator();
    }

    public boolean nextFile()
    {
        if (!files.hasNext()) {
            return false;
        }
        currentBuffers = files.next().iterator();
        return true;
    }

    public Buffer poll()
    {
        if (currentBuffers == null) {
            throw new IllegalStateException("FileInput.nextFile is not called");
        }
        if (!currentBuffers.hasNext()) {
            return null;
        }
        return currentBuffers.next();
    }

    public void close()
    {
        do {
            while (true) {
                Buffer b = poll();
                if (b == null) {
                    break;
                }
                b.release();
            }
        } while (nextFile());
    }
}
