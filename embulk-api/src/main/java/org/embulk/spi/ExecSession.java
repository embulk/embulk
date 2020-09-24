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
 * <p>Some methods, such as {@code newPlugin}, are deprecated. Those methods will be removed soon during Embulk v0.10 when
 * this {@link Exec} moves to {@code embulk-api} from {@code embulk-core}.
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

    public abstract ExecSession forPreview();

    @Deprecated
    public abstract ConfigSource getSessionExecConfig();

    @Deprecated  // https://github.com/embulk/embulk/issues/1292
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public abstract org.embulk.spi.time.Timestamp getTransactionTime();

    public abstract Instant getTransactionTimeInstant();

    public abstract String getTransactionTimeString();

    @Deprecated  // @see docs/design/slf4j.md
    public abstract Logger getLogger(String name);

    @Deprecated  // @see docs/design/slf4j.md
    public abstract Logger getLogger(Class<?> clazz);

    public abstract BufferAllocator getBufferAllocator();

    public abstract PageBuilder getPageBuilder(final BufferAllocator allocator, final Schema schema, final PageOutput output);

    public abstract PageReader getPageReader(final Schema schema);

    public abstract TaskReport newTaskReport();

    public abstract ConfigDiff newConfigDiff();

    public abstract ConfigSource newConfigSource();

    public abstract TaskSource newTaskSource();

    public abstract TempFileSpace getTempFileSpace();

    public abstract boolean isPreview();

    public abstract void cleanup();
}
