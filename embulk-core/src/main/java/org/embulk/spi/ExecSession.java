package org.embulk.spi;

import com.google.inject.Injector;
import java.time.Instant;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.spi.time.TimestampFormatter;
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

    @Deprecated
    public static class Builder {
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    public static Builder builder(Injector injector) {
        throw new UnsupportedOperationException(
                "ExecSession.builder is no longer supported. "
                + "If it is really needed, a plugin can call ExecSessionInternal.builder instead. "
                + "But, keep it in mind that ExecSessionInternal is strongly discouraged for plugins to call. "
                + "Embulk does not guarantee any compatibility with ExecSessionInternal. "
                + "If you are trying to access ExecSessionInternal from your plugin's non-test code, "
                + "remember that your plugin may easily stop working from some version of Embulk.");
    }

    @Deprecated
    public ExecSession(Injector injector, ConfigSource configSource) {
        throw new UnsupportedOperationException(
                "ExecSession's constructor is no longer supported. "
                + "If it is really needed, a plugin can call ExecSessionInternal.builder instead. "
                + "But, keep it in mind that ExecSessionInternal is strongly discouraged for plugins to call. "
                + "Embulk does not guarantee any compatibility with ExecSessionInternal. "
                + "If you are trying to access ExecSessionInternal from your plugin's non-test code, "
                + "remember that your plugin may easily stop working from some version of Embulk.");
    }

    public abstract ExecSession forPreview();

    @Deprecated
    public abstract ConfigSource getSessionExecConfig();

    /**
     * Returns Embulk's internal Guice Injector.
     *
     * <p>This method is deprecated, and it will be removed from {@link ExecSession} very soon during Embulk v0.10
     * although it will stay in {@link ExecSessionInternal}.
     *
     * @deprecated {@code getInjector} will be removed from {@link ExecSession} very soon during Embulk v0.10.
     */
    @Deprecated  // https://github.com/embulk/embulk/issues/1313
    public abstract Injector getInjector();

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

    /**
     * Returns Embulk's {@link org.embulk.config.ModelManager}.
     *
     * <p>This method is deprecated, and it will be removed from {@link ExecSession} very soon during Embulk v0.10
     * although it will stay in {@link ExecSessionInternal}.
     *
     * @deprecated {@code getModelManager} will be removed from {@link Exec} very soon during Embulk v0.10.
     */
    @Deprecated  // https://github.com/embulk/embulk/issues/1304
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public abstract org.embulk.config.ModelManager getModelManager();

    /**
     * Registers a new Embulk plugin.
     *
     * <p>This method is deprecated, and it will be removed from {@link ExecSession} very soon during Embulk v0.10
     * although it will stay in {@link ExecSessionInternal}.
     *
     * @deprecated {@code newPlugin} will be removed from {@link Exec} very soon during Embulk v0.10.
     */
    @Deprecated  // https://github.com/embulk/embulk/issues/1309
    public abstract <T> T newPlugin(Class<T> iface, PluginType type);

    public abstract TaskReport newTaskReport();

    public abstract ConfigDiff newConfigDiff();

    public abstract ConfigSource newConfigSource();

    public abstract TaskSource newTaskSource();

    // To be removed by v0.10 or earlier.
    @Deprecated  // https://github.com/embulk/embulk/issues/936
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/936
    public abstract TimestampFormatter newTimestampFormatter(String format, org.joda.time.DateTimeZone timezone);

    public abstract TempFileSpace getTempFileSpace();

    public abstract boolean isPreview();

    public abstract void cleanup();
}
