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
 * Provides static access inside Embulk for plugins.
 *
 * <p>Some methods, {@code getInjector}, {@code getModelManager}, and {@code newPlugin} have been removed since {@code v0.10.18}.
 *
 * <p>If access to the removed methods is really needed, a plugin can technically call them via {@code ExecInternal}
 * in {@code embulk-core}. It is the quickest way to catch-up with the latest Embulk v0.10. But once doing it, the plugin won't
 * work with Embulk v0.9. To keep your plugin work for both, find another way to realize what you want.
 *
 * <p>Keeping on calling {@code ExecInternal} is strongly discouraged although it can be a short-term solution as shown above.
 * Embulk does not guarantee any compatibility in {@code embulk-core} with plugins. The plugin may easily stop working at some
 * point of Embulk versions. Do it at your own risk.
 */
public class Exec {
    private static final InheritableThreadLocal<ExecSession> session = new InheritableThreadLocal<ExecSession>();

    private Exec() {}

    /**
     * To be called only from {@code ExecInternal.doWith}.
     */
    static void setThreadLocalSession(final ExecSession session) {
        Exec.session.set(session);
    }

    /**
     * Returns the registered {@link ExecSession} instance.
     *
     * @deprecated Plugins are discouraged from getting the {@link ExecSession} instance.
     */
    @Deprecated
    public static ExecSession session() {
        return sessionForInside();
    }

    private static ExecSession sessionForInside() {
        ExecSession session = Exec.session.get();
        if (session == null) {
            throw new NullPointerException("Exec is used outside of ExecInternal.doWith.");
        }
        return session;
    }

    /**
     * Returns the transaction time in {@link org.embulk.spi.time.Timestamp}.
     *
     * @deprecated {@link org.embulk.spi.time.Timestamp} is deprecated.
     */
    @Deprecated  // https://github.com/embulk/embulk/issues/1292
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public static org.embulk.spi.time.Timestamp getTransactionTime() {
        return org.embulk.spi.time.Timestamp.ofInstant(sessionForInside().getTransactionTimeInstant());
    }

    /**
     * Returns the transaction time in {@link java.time.Instant}.
     */
    public static Instant getTransactionTimeInstant() {
        return sessionForInside().getTransactionTimeInstant();
    }

    /**
     * Returns a SLF4J {@link org.slf4j.Logger} instance named according to the name parameter.

     * @param name  the name of the {@link org.slf4j.Logger}
     * @return the {@link org.slf4j.Logger} instance
     *
     * @deprecated Call {@link org.slf4j.LoggerFactory#getLogger(String)} directly, instead.
     */
    @Deprecated  // @see docs/design/slf4j.md
    @SuppressWarnings("deprecation")
    public static Logger getLogger(String name) {
        return sessionForInside().getLogger(name);
    }

    /**
     * Returns a SLF4J {@link org.slf4j.Logger} instance named corresponding to the class passed as parameter.

     * @param clazz  the returned {@link org.slf4j.Logger} will be named after {@code clazz}
     * @return the {@link org.slf4j.Logger} instance
     *
     * @deprecated Call {@link org.slf4j.LoggerFactory#getLogger(Class)} directly, instead.
     */
    @Deprecated  // @see docs/design/slf4j.md
    @SuppressWarnings("deprecation")
    public static Logger getLogger(Class<?> clazz) {
        return sessionForInside().getLogger(clazz);
    }

    /**
     * Returns the {@link BufferAllocator} instance.
     */
    public static BufferAllocator getBufferAllocator() {
        return sessionForInside().getBufferAllocator();
    }

    /**
     * Returns a {@link PageBuilder} instance created for the parameters.
     */
    public static PageBuilder getPageBuilder(final BufferAllocator allocator, final Schema schema, final PageOutput output) {
        return sessionForInside().getPageBuilder(allocator, schema, output);
    }

    /**
     * Returns a {@link PageReader} instance created for the parameter.
     */
    public static PageReader getPageReader(final Schema schema) {
        return sessionForInside().getPageReader(schema);
    }

    /**
     * Creates a new empty {@link org.embulk.config.TaskReport} instance.
     *
     * @deprecated Recommended to start using {@code embulk-util-config} from plugins. Use its own {@code newTaskReport}.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static TaskReport newTaskReport() {
        return sessionForInside().newTaskReport();
    }

    /**
     * Creates a new empty {@link org.embulk.config.ConfigDiff} instance.
     *
     * @deprecated Recommended to start using {@code embulk-util-config} from plugins. Use its own {@code newConfigDiff}.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static ConfigDiff newConfigDiff() {
        return sessionForInside().newConfigDiff();
    }

    /**
     * Creates a new empty {@link org.embulk.config.ConfigSource} instance.
     *
     * @deprecated Recommended to start using {@code embulk-util-config} from plugins. Use its own {@code newConfigSource}.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static ConfigSource newConfigSource() {
        return sessionForInside().newConfigSource();
    }

    /**
     * Creates a new empty {@link org.embulk.config.TaskSource} instance.
     *
     * @deprecated Recommended to start using {@code embulk-util-config} from plugins. Use its own {@code newTaskSource}.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static TaskSource newTaskSource() {
        return sessionForInside().newTaskSource();
    }

    /**
     * Returns a space for temporary files, a {@link org.embulk.spi.TempFileSpace} instance corresponding to the registered
     * {@link ExecSession}.
     */
    public static TempFileSpace getTempFileSpace() {
        return sessionForInside().getTempFileSpace();
    }

    /**
     * Returns {@code true} if the execution is in a preview mode.
     */
    public static boolean isPreview() {
        return sessionForInside().isPreview();
    }
}
