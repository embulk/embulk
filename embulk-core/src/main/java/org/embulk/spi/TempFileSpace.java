package org.embulk.spi;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
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

    public void cleanup() {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File e : files) {
                try {
                    deleteFileRecursively(e);
                } catch (IOException ex) {
                    // ignore IOException
                }
            }
        }
        dir.delete();
        dirCreated = false;
    }

    private void deleteFileRecursively(File file) throws IOException {
        Path directory = file.toPath();
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
