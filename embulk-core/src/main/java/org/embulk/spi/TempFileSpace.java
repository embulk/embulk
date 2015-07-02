package org.embulk.spi;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.IOException;
import com.google.common.base.Preconditions;

public class TempFileSpace
{
    private final File dir;
    private boolean dirCreated;

    public TempFileSpace(File dir)
    {
        Preconditions.checkArgument(dir != null, "dir is null");
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

    public void cleanup()
    {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File e : files) {
                e.delete();
                // TODO delete directory recursively
            }
        }
        dir.delete();
        dirCreated = false;
    }
}
