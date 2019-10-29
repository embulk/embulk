package org.embulk.spi;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible to manage a temporary directory (a space for temporary files) for a task.
 *
 * <p>Plugins are expected to get this through {@link Exec#getTempFileSpace}. Do not create an instance of this directly.
 */
public class TempFileSpace {
    private TempFileSpace(final boolean isUnique, final Path baseDir, final String prefix, final File exactDir) {
        this.isUnique = isUnique;
        if (isUnique) {
            this.baseDir = baseDir;
            this.prefix = prefix;
            this.exactDir = null;
        } else {  // Backward-compatible behavior for `new TempFileSpace(File)`. To be removed.
            if (exactDir == null) {
                throw new IllegalArgumentException("dir is null");
            }
            this.baseDir = null;
            this.prefix = null;
            this.exactDir = exactDir;
        }

        this.tempDirectoryCreated = Optional.empty();
    }

    /**
     * Creates an instance of {@link TempFileSpace}.
     *
     * <p>It creates {@link TempFileSpace} exactly at the specified {@link java.io.File}.
     *
     * <p>Note that the corresponding directory is created lazily when the first temporary file is created there.
     *
     * @deprecated It has been deprecated because it may use the same directory from different processes and/or tasks.
     * Use {@link #with(java.nio.file.Path, java.lang.String)} instead. It is to be removed.
     */
    @Deprecated
    public TempFileSpace(final File dir) {
        this(false, null, null, dir);
    }

    /**
     * Creates an instance of {@link TempFileSpace}.
     *
     * <p>Note that the corresponding directory is created lazily when the first temporary file is created there.
     *
     * <p>It creates {@link TempFileSpace} under the specified {@code baseDir} with the {@code prefix}.
     */
    public static TempFileSpace with(final Path baseDir, final String prefix) throws IOException {
        if (baseDir == null || prefix == null) {
            throw new IllegalArgumentException("TempFileSpace is created with null.");
        }
        if (!baseDir.isAbsolute()) {
            throw new IllegalArgumentException("TempFileSpace is created under a relative path.");
        }
        if (!Files.isDirectory(baseDir)) {  // Following symlinks -- no LinkOption.NOFOLLOW_LINKS.
            throw new IOException("TempFileSpace is created under non-directory.");
        }

        return new TempFileSpace(true, baseDir, prefix, null);
    }

    /**
     * Creates a temporary file with the default file name prefix, and the default file extension suffix.
     *
     * @return A temporary {@link java.io.File} created
     */
    public File createTempFile() {
        return this.createTempFile("tmp");
    }

    /**
     * Creates a temporary file with the default file name prefix, and the specified file extension suffix.
     *
     * @param fileExt  The file extension suffix without a leading dot ({@code '.'})
     * @return A temporary {@link java.io.File} created
     */
    public File createTempFile(final String fileExt) {
        // Thread names contain ':' which is not valid as file names in Windows.
        return this.createTempFile(Thread.currentThread().getName().replaceAll(":", "_") + "_", fileExt);
    }

    /**
     * Creates a temporary file with the specified file name prefix, and the specified file extension suffix.
     *
     * @param prefix  The file name prefix
     * @param fileExt  The file extension suffix without a leading dot ({@code '.'})
     * @return A temporary {@link java.io.File} created
     */
    public synchronized File createTempFile(final String prefix, final String fileExt) {
        try {
            this.createTempDirectoryIfRequired();

            final Path tempFile = Files.createTempFile(this.tempDirectoryCreated.get(), prefix, "." + fileExt);
            logger.debug("TempFile \"{}\" is created.", tempFile);
            return tempFile.toFile();
        } catch (final IOException ex) {
            throw new TempFileException(ex);
        }
    }

    /**
     * Cleans up the temporary file space, and everything in the space.
     *
     * <p>It is to be called along with Embulk's cleanup process. Plugins should not call this directly.
     */
    public synchronized void cleanup() {
        if (this.tempDirectoryCreated.isPresent()) {
            logDebugWithStackTraces("TempFileSpace \"" + this.tempDirectoryCreated.get().toString() + "\" is cleaned up at");
        }

        try {
            if (this.tempDirectoryCreated.isPresent()) {
                this.deleteFilesIfExistsRecursively(this.tempDirectoryCreated.get());
            }
        } catch (final IOException ex) {
            // ignore IOException
        }
        this.tempDirectoryCreated = Optional.empty();
    }

    private void deleteFilesIfExistsRecursively(final Path dirToDelete) throws IOException {
        Files.walkFileTree(dirToDelete, new SimpleFileVisitor<Path>() {
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

    private void createTempDirectoryIfRequired() throws IOException {
        if (this.tempDirectoryCreated.isPresent()) {
            if (logger.isDebugEnabled()) {
                logger.debug("TempFileSpace \"{}\" is already there.", this.tempDirectoryCreated.get());
            }
            return;
        }

        if (this.baseDir != null) {
            this.tempDirectoryCreated = Optional.of(Files.createTempDirectory(this.baseDir, this.prefix));
        } else if (this.exactDir != null) {
            this.exactDir.mkdirs();
            this.tempDirectoryCreated = Optional.of(this.exactDir.toPath());
        }
    }

    private static void logDebugWithStackTraces(final String message) {
        if (logger.isDebugEnabled()) {
            final StringBuilder builder = new StringBuilder();
            builder.append(message).append(" :");
            for (final StackTraceElement stack : (new Throwable()).getStackTrace()) {
                builder.append("\n  > ").append(stack.toString());
            }
            logger.debug(builder.toString());
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(TempFileSpace.class);

    // true if it is created through TempFileSpace.with.
    //
    // TODO: Remove it once the constructor TempFileSpace(File) goes away.
    private final boolean isUnique;

    // The base directory to create a temporary directory under this.
    //
    // Available only when created through TempFileSpace.with.
    private final Path baseDir;

    // The prefix when creating a temporary directory under |baseDir|.
    //
    // Available only when created through TempFileSpace.with.
    private final String prefix;

    // The exact directory specified through the constructor TempFileSpace(File).
    //
    // Available only when created through the constructor TempFileSpace(File).
    //
    // TODO: Remove it once the constructor TempFileSpace(File) goes away.
    private final File exactDir;

    // The temporary directory created when creating the first temporary file.
    private Optional<Path> tempDirectoryCreated;
}
