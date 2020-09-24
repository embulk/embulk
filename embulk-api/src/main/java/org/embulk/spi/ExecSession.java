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

import java.time.Instant;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.slf4j.Logger;

/**
 * Provides access inside Embulk for plugins through a transactional session.
 *
 * <p>Some methods, {@code getInjector}, {@code getModelManager}, and {@code newPlugin} have been removed since {@code v0.10.18}.
 *
 * <p>Accessing {@link ExecSession} from plugins is however discouraged though {@link ExecSession} is in {@code embulk-api},
 * so accessible from plugins.
 */
public abstract class ExecSession {
    /**
     * A constructor for {@link ExecSessionInternal} inherited.
     */
    ExecSession() {
    }

    /**
     * Clones this {@link ExecSession} instance in a preview mode.
     */
    public abstract ExecSession forPreview();

    /**
     * Returns a {@link org.embulk.config.ConfigSource} instance that includes {@code transaction_time} for the {@code exec} part.
     *
     * @deprecated Plugins should no longer use it. It would be removed.
     */
    @Deprecated
    public abstract ConfigSource getSessionExecConfig();

    /**
     * Returns the transaction time in {@link org.embulk.spi.time.Timestamp}.
     *
     * @deprecated {@link org.embulk.spi.time.Timestamp} is deprecated.
     */
    @Deprecated  // https://github.com/embulk/embulk/issues/1292
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public abstract org.embulk.spi.time.Timestamp getTransactionTime();

    /**
     * Returns the transaction time in {@link java.time.Instant}.
     */
    public abstract Instant getTransactionTimeInstant();

    /**
     * Returns the transaction time formatted as a {@link java.lang.String}.
     */
    public abstract String getTransactionTimeString();

    /**
     * Returns a SLF4J {@link org.slf4j.Logger} instance named according to the name parameter.

     * @param name  the name of the {@link org.slf4j.Logger}
     * @return the {@link org.slf4j.Logger} instance
     *
     * @deprecated Call {@link org.slf4j.LoggerFactory#getLogger(String)} directly, instead.
     */
    @Deprecated  // @see docs/design/slf4j.md
    public abstract Logger getLogger(String name);

    /**
     * Returns a SLF4J {@link org.slf4j.Logger} instance named corresponding to the class passed as parameter.

     * @param clazz  the returned {@link org.slf4j.Logger} will be named after {@code clazz}
     * @return the {@link org.slf4j.Logger} instance
     *
     * @deprecated Call {@link org.slf4j.LoggerFactory#getLogger(Class)} directly, instead.
     */
    @Deprecated  // @see docs/design/slf4j.md
    public abstract Logger getLogger(Class<?> clazz);

    /**
     * Returns the {@link BufferAllocator} instance.
     */
    public abstract BufferAllocator getBufferAllocator();

    /**
     * Returns a {@link PageBuilder} instance created for the parameters.
     */
    public abstract PageBuilder getPageBuilder(final BufferAllocator allocator, final Schema schema, final PageOutput output);

    /**
     * Returns a {@link PageReader} instance created for the parameter.
     */
    public abstract PageReader getPageReader(final Schema schema);

    /**
     * Creates a new empty {@link org.embulk.config.TaskReport} instance.
     *
     * @deprecated Recommended to start using {@code embulk-util-config} from plugins. Use its own {@code newTaskReport}.
     */
    @Deprecated
    public abstract TaskReport newTaskReport();

    /**
     * Creates a new empty {@link org.embulk.config.ConfigDiff} instance.
     *
     * @deprecated Recommended to start using {@code embulk-util-config} from plugins. Use its own {@code newConfigDiff}.
     */
    @Deprecated
    public abstract ConfigDiff newConfigDiff();

    /**
     * Creates a new empty {@link org.embulk.config.ConfigSource} instance.
     *
     * @deprecated Recommended to start using {@code embulk-util-config} from plugins. Use its own {@code newConfigSource}.
     */
    @Deprecated
    public abstract ConfigSource newConfigSource();

    /**
     * Creates a new empty {@link org.embulk.config.TaskSource} instance.
     *
     * @deprecated Recommended to start using {@code embulk-util-config} from plugins. Use its own {@code newTaskSource}.
     */
    @Deprecated
    public abstract TaskSource newTaskSource();

    /**
     * Returns a space for temporary files, a {@link org.embulk.spi.TempFileSpace} instance corresponding to the registered
     * {@link ExecSession}.
     */
    public abstract TempFileSpace getTempFileSpace();

    /**
     * Returns {@code true} if the execution is in a preview mode.
     */
    public abstract boolean isPreview();

    /**
     * Cleans up the session.
     */
    public abstract void cleanup();
}
