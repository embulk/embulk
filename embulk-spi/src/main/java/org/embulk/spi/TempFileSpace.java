/*
 * Copyright 2015 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.spi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Manages a space for temporary files.
 *
 * <p>Plugins are expected to get this through {@link org.embulk.spi.Exec#getTempFileSpace}.
 *
 * @since 0.6.16
 */
public abstract class TempFileSpace {
    /**
     * Exists only for {@code TempFileSpaceImpl}'s constructor as {@link TempFileSpace} has another deprecated constructor.
     */
    TempFileSpace() {
    }

    /**
     * @deprecated The constructor is no longer available. Use {@link org.embulk.spi.Exec#getTempFileSpace} instead.
     *
     * @since 0.6.16
     */
    @Deprecated
    public TempFileSpace(final File dir) {
        throw new UnsupportedOperationException(
                "TempFileSpace can no longer be constructed directly. Use Exec.getTempFileSpace() instead.");
    }

    /**
     * @deprecated The static creator method is no longer available. Use {@link org.embulk.spi.Exec#getTempFileSpace} instead.
     *
     * @since 0.9.20
     */
    @Deprecated
    public static TempFileSpace with(final Path baseDir, final String prefix) throws IOException {
        throw new UnsupportedOperationException(
                "TempFileSpace.with can no longer be called directly. Use Exec.getTempFileSpace() instead.");
    }

    /**
     * Creates a temporary file in the space, with the default file name prefix, and the default file extension suffix.
     *
     * @return a temporary {@link java.io.File} created
     *
     * @since 0.6.16
     */
    public abstract File createTempFile();

    /**
     * Creates a temporary file in the space, with the default file name prefix, and the specified file extension suffix.
     *
     * @param fileExt  the file extension suffix without a leading dot ({@code '.'})
     * @return a temporary {@link java.io.File} created
     *
     * @since 0.6.16
     */
    public abstract File createTempFile(final String fileExt);

    /**
     * Creates a temporary file in the space, with the specified file name prefix, and the specified file extension suffix.
     *
     * @param prefix  the file name prefix
     * @param fileExt  the file extension suffix without a leading dot ({@code '.'})
     * @return a temporary {@link java.io.File} created
     *
     * @since 0.6.16
     */
    public abstract File createTempFile(final String prefix, final String fileExt);

    /**
     * Cleans up the temporary file space, and everything in the space.
     *
     * <p>It is to be called along with Embulk's cleanup process. Plugins should not call this directly.
     *
     * @since 0.6.16
     */
    public abstract void cleanup();
}
