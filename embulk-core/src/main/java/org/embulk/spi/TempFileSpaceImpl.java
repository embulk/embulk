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

public class TempFileSpaceImpl extends TempFileSpace {
    private TempFileSpaceImpl(final Path baseDir, final String prefix) {
        this.baseDir = baseDir;
        this.prefix = prefix;
        this.tempDirectoryCreated = Optional.empty();
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1319
    public static TempFileSpaceImpl with(final Path baseDir, final String prefix) throws IOException {
        if (baseDir == null || prefix == null) {
            throw new IllegalArgumentException("TempFileSpace cannot be created with null.");
        }
        if (!baseDir.isAbsolute()) {
            throw new IllegalArgumentException("TempFileSpace cannot be created under a relative path.");
        }
        if (!Files.isDirectory(baseDir)) {  // Following symlinks -- no LinkOption.NOFOLLOW_LINKS.
            throw new IOException("TempFileSpace cannot be created under non-directory.");
        }

        return new TempFileSpaceImpl(baseDir, prefix);
    }

    @Override
    public File createTempFile() {
        return this.createTempFile("tmp");
    }

    @Override
    public File createTempFile(final String fileExt) {
        // Thread names contain ':' which is not valid as file names in Windows.
        return this.createTempFile(Thread.currentThread().getName().replaceAll(":", "_") + "_", fileExt);
    }

    @Override
    public synchronized File createTempFile(final String prefix, final String fileExt) {
        try {
            this.createTempDirectoryIfRequired();

            final Path tempFile;
            try {
                tempFile = Files.createTempFile(this.tempDirectoryCreated.get(), prefix, "." + fileExt);
            } catch (final IllegalArgumentException ex) {
                throw new IOException(
                        "Failed to create a temp file with illegal prefix or suffix given. "
                                + "For example, \"/\" is not accepted in prefix nor suffix since Embulk v0.9.20. "
                                + "Please advise the plugin developer about it. "
                                + "(prefix: \"" + prefix + "\", suffix: \"" + fileExt + "\")",
                        ex);
            }

            logger.debug("TempFile \"{}\" is created.", tempFile);
            return tempFile.toFile();
        } catch (final IOException ex) {
            throw new TempFileException(ex);
        }
    }

    @Override
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

    Optional<Path> getTempDirectoryForTesting() {
        return this.tempDirectoryCreated;
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

    private static final Logger logger = LoggerFactory.getLogger(TempFileSpaceImpl.class);

    // The base directory to create a temporary directory under this.
    //
    // Available only when created through TempFileSpace.with.
    private final Path baseDir;

    // The prefix when creating a temporary directory under |baseDir|.
    //
    // Available only when created through TempFileSpace.with.
    private final String prefix;

    // The temporary directory created when creating the first temporary file.
    private Optional<Path> tempDirectoryCreated;
}
