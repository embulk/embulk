package org.embulk.spi;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempFileSpace {
    public TempFileSpace(final File dir) {
        if (dir == null) {
            throw new IllegalArgumentException("dir is null");
        }
        this.dir = dir;
    }

    public File createTempFile() {
        return this.createTempFile("tmp");
    }

    public File createTempFile(final String fileExt) {
        // Thread names contain ':' which is not valid as file names in Windows.
        return this.createTempFile(Thread.currentThread().getName().replaceAll(":", "_") + "_", fileExt);
    }

    public synchronized File createTempFile(final String prefix, final String fileExt) {
        try {
            if (!this.dirCreated) {
                this.dir.mkdirs();
                this.dirCreated = true;
            }
            return File.createTempFile(prefix, "." + fileExt, this.dir);
        } catch (final IOException ex) {
            throw new TempFileException(ex);
        }
    }

    public synchronized void cleanup() {
        if (this.dirCreated && logger.isDebugEnabled()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("TempFileSpace \"").append(this.dir.toString()).append("\" is cleaned up at :");
            for (final StackTraceElement stack : (new Throwable()).getStackTrace()) {
                builder.append("\n  > ").append(stack.toString());
            }
            logger.debug(builder.toString());
        }

        try {
            this.deleteFilesIfExistsRecursively(this.dir);
        } catch (final IOException ex) {
            // ignore IOException
        }
        this.dirCreated = false;
    }

    private void deleteFilesIfExistsRecursively(final File dirToDelete) throws IOException {
        Files.walkFileTree(dirToDelete.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path fileOnVisit, final BasicFileAttributes attrs) {
                    try {
                        Files.deleteIfExists(fileOnVisit);
                    } catch (final IOException ex) {
                        // ignore IOException
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dirOnVisit, final IOException exc) {
                    try {
                        Files.deleteIfExists(dirOnVisit);
                    } catch (IOException ex) {
                        // ignore IOException
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
    }

    private static final Logger logger = LoggerFactory.getLogger(TempFileSpace.class);

    private final File dir;
    private boolean dirCreated;
}
