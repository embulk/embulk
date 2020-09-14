package org.embulk.spi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Responsible to manage a temporary directory (a space for temporary files) for a task.
 *
 * <p>Plugins are expected to get this through {@link Exec#getTempFileSpace}. Do not create an instance of this directly.
 */
public abstract class TempFileSpace {
    TempFileSpace() {
    }

    @Deprecated
    public TempFileSpace(final File dir) {
        throw new UnsupportedOperationException(
                "TempFileSpace can no longer be constructed directly. Use Exec.getTempFileSpace() instead.");
    }

    @Deprecated
    public static TempFileSpace with(final Path baseDir, final String prefix) throws IOException {
        throw new UnsupportedOperationException(
                "TempFileSpace.with can no longer be called directly. Use Exec.getTempFileSpace() instead.");
    }

    /**
     * Creates a temporary file with the default file name prefix, and the default file extension suffix.
     *
     * @return A temporary {@link java.io.File} created
     */
    public abstract File createTempFile();

    /**
     * Creates a temporary file with the default file name prefix, and the specified file extension suffix.
     *
     * @param fileExt  The file extension suffix without a leading dot ({@code '.'})
     * @return A temporary {@link java.io.File} created
     */
    public abstract File createTempFile(final String fileExt);

    /**
     * Creates a temporary file with the specified file name prefix, and the specified file extension suffix.
     *
     * @param prefix  The file name prefix
     * @param fileExt  The file extension suffix without a leading dot ({@code '.'})
     * @return A temporary {@link java.io.File} created
     */
    public abstract File createTempFile(final String prefix, final String fileExt);

    /**
     * Cleans up the temporary file space, and everything in the space.
     *
     * <p>It is to be called along with Embulk's cleanup process. Plugins should not call this directly.
     */
    public abstract void cleanup();
}
