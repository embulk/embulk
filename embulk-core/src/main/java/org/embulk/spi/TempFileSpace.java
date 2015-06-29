package org.embulk.spi;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.IOException;

public class TempFileSpace
{
    private final File dir;
    private boolean dirCreated;

    public TempFileSpace(File dir)
    {
        this.dir = dir;
    }

    public File createTempFile()
    {
        return createTempFile("tmp");
    }

    public File createTempFile(String fileExt)
    {
        return createTempFile(Thread.currentThread().getName()+"_", fileExt);
    }

    public File createTempFile(String prefix, String fileExt)
    {
        try {
            if (!dirCreated) {
                dir.mkdirs();
                dirCreated = true;
            }
            return File.createTempFile(prefix, "."+fileExt, dir);
        } catch (IOException ex) {
            throw new TempFileException(ex);
        }
    }

    public void clean()
    {
        for (File e : dir.listFiles()) {
            e.delete();
        }
        dir.delete();
    }
}
