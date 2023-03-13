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

import org.embulk.config.TaskReport;

/**
 * Represents a series of file-like byte sequence inputs in a transaction from a File Input Plugin.
 *
 * @since 0.4.0
 */
public interface TransactionalFileInput extends Transactional, FileInput {
    /**
     * Switches the {@link org.embulk.spi.TransactionalFileInput} to process the next file.
     *
     * @return {@code true} if this {@link org.embulk.spi.TransactionalFileInput} has switched to the next file. {@code false} if no more files.
     *
     * @since 0.4.0
     */
    @Override
    boolean nextFile();

    /**
     * Reads a byte sequence from this {@link org.embulk.spi.TransactionalFileInput} into {@link org.embulk.spi.Buffer}.
     *
     * @return the {@link org.embulk.spi.Buffer} read
     *
     * @since 0.4.0
     */
    @Override
    Buffer poll();

    /**
     * Closes this {@link org.embulk.spi.TransactionalFileInput}.
     *
     * @since 0.4.0
     */
    @Override
    void close();

    /**
     * Aborts the transaction of {@link org.embulk.spi.TransactionalFileInput}.
     *
     * @since 0.4.0
     */
    @Override
    void abort();

    /**
     * Commits the transaction of {@link org.embulk.spi.TransactionalFileInput}.
     *
     * @return report
     *
     * @since 0.7.0
     */
    @Override
    TaskReport commit();
}
