package org.embulk.spi;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Space managing Embulk's temporary files.
 */
public class TempFileSpace {
    private static final Logger logger = LoggerFactory.getLogger(TempFileSpace.class);

    private static final String TMP_FILE_EXT = "tmp";

    /**
     * Temporary files directory.
     */
    private final File dir;

    /**
     * Creates a new instance of the temporary file space.
     *
     * @param dir Target directory for temporary file generation.
     * @throws IllegalArgumentException If the target directory is null.
     */
    public TempFileSpace(final File dir) {
        if (dir == null) {
            throw new IllegalArgumentException("dir is null");
        }
        this.dir = dir;
    }

    /**
     * Creates a temporary file with {@link #TMP_FILE_EXT} extension.
     *
     * @return Temporary file.
     */
    public File createTempFile() {
        return createTempFile(TMP_FILE_EXT);
    }

    /**
     * Creates a temporary file with the current thread's name as prefix and the specified extension.
     *
     * @param fileExt File extension.
     * @return Temporary file.
     */
    public File createTempFile(final String fileExt) {
        // Thread names contain ':' which is not valid as file names in Windows.
        Function<String, String> normalizeName = fileName -> fileName.replaceAll(":", "_") + "_";
        return this.createTempFile(normalizeName.apply(Thread.currentThread().getName()), fileExt);
    }

    /**
     * Creates a temporary file with the specified prefix and extension.
     *
     * @param prefix  File prefix.
     * @param fileExt File extension.
     * @return Temporary file.
     */
    public synchronized File createTempFile(final String prefix, final String fileExt) {
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }

            return File.createTempFile(prefix, "." + fileExt, dir);
        } catch (final IOException ex) {
            throw new TempFileException(ex);
        }
    }

    /**
     * Deletes all temporary files.
     */
    public synchronized void cleanup() {
        try {
            this.deleteFilesIfExistsRecursively(this.dir);
        } catch (final IOException ex) {
            logger.warn("Failed to cleanup");
        }
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
}
