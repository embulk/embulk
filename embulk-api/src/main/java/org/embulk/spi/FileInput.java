/*
 * Copyright 2014 The Embulk project
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

import java.util.Optional;

/**
 * Represents a series of file-like byte sequence inputs from a File Input Plugin.
 *
 * @since 0.4.0
 */
public interface FileInput extends AutoCloseable {
    /**
     * Switches the {@link org.embulk.spi.FileInput} to process the next file.
     *
     * @return {@code true} if this {@link org.embulk.spi.FileInput} has switched to the next file. {@code false} if no more files.
     *
     * @since 0.4.0
     */
    boolean nextFile();

    /**
     * Reads a byte sequence from this {@link org.embulk.spi.FileInput} into {@link org.embulk.spi.Buffer}.
     *
     * @return the {@link org.embulk.spi.Buffer} read
     *
     * @since 0.4.0
     */
    Buffer poll();

    /**
     * Closes this {@link org.embulk.spi.FileInput}.
     *
     * @since 0.4.0
     */
    @Override
    void close();

    /**
     * Gets a {@link java.lang.String} that hints the name of the current file input.
     *
     * <p>The hint is only for human-readable logs, not to be stored as any record. Nothing
     * is guaranteed in the hint. Plugins may change the hint without any notification when
     * upgraded. The hint can be lost through a chain of plugin calls -- File Input Plugin,
     * Decoder Plugins, and Parser Plugin.
     *
     * @return the hinting {@link java.lang.String} in {@link java.util.Optional}
     *
     * @since 0.9.12
     */
    default Optional<String> hintOfCurrentInputFileNameForLogging() {
        return Optional.empty();
    }

    default LineageMetadata lineageMetadata() {
        return LineageMetadata.of();
    }
}
