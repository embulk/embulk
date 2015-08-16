package org.embulk.spi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
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
        try {
            deleteFilesIfExistsRecursively(dir);
        }
        catch (IOException ex) {
            // ignore IOException
        }
        dirCreated = false;
    }

    private void deleteFilesIfExistsRecursively(File dir)
            throws IOException
    {
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                try {
                    Files.deleteIfExists(file);
                }
                catch (IOException ex) {
                    // ignore IOException
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            {
                try {
                    Files.deleteIfExists(dir);
                }
                catch (IOException ex) {
                    // ignore IOException
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
