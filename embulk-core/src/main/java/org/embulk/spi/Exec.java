package org.embulk.spi;

import com.google.inject.Injector;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.slf4j.Logger;

/**
 * Provides static access inside Embulk for plugins.
 *
 * <p>Some methods, such as {@code newPlugin}, are deprecated. Those methods will be removed soon during Embulk v0.10 when
 * this {@link Exec} moves to {@code embulk-api} from {@code embulk-core}.
 *
 * <p>If access to the deprecated/removed methods is really needed, a plugin can technically call them via {@link ExecInternal}
 * in {@code embulk-core}. It is the quickest way to catch-up with the latest Embulk v0.10. But once doing it, the plugin won't
 * work with Embulk v0.9. To keep your plugin work for both, find another way to realize what you want.
 *
 * <p>Keeping on calling {@link ExecInternal} is strongly discouraged although it can be a short-term solution as shown above.
 * Embulk does not guarantee any compatibility in {@code embulk-core} with plugins. The plugin may easily stop working at some
 * point of Embulk versions. Do it at your own risk.
 */
public class Exec {
    private static final InheritableThreadLocal<ExecSession> session = new InheritableThreadLocal<ExecSession>();

    private Exec() {}

    @Deprecated
    public static <T> T doWith(ExecSession session, ExecAction<T> action) throws ExecutionException {
        throw new ExecutionException(new UnsupportedOperationException(
                "Exec.doWith is no longer supported. "
                + "If it is really needed, a plugin can call ExecInternal.doWith instead. "
                + "But, keep it in mind that ExecInternal is strongly discouraged for plugins to call. "
                + "Embulk does not guarantee any compatibility with ExecInternal. "
                + "If you are trying to access ExecInternal from your plugin's non-test code, "
                + "remember that your plugin may easily stop working from some version of Embulk."));
    }

    /**
     * To be called only from ExecInternal.doWith.
     */
    static void setThreadLocalSession(final ExecSession session) {
        Exec.session.set(session);
    }

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
     * Returns Embulk's internal Guice Injector.
     *
     * <p>This method is deprecated, and it will be removed from {@link Exec} very soon during Embulk v0.10. If a plugin
     * really needs it, a plugin can technically {@link ExecInternal#getInjector} in {@code embulk-core} instead.
     *
     * <p>But, it is strongly discouraged to keep on calling it for a long time. Embulk does not guarantee any compatibility in
     * {@code embulk-core} with plugins. If a plugin calls it, the plugin may stop working at some point of Embulk versions.
     * Do it at your own risk.
     *
     * @deprecated {@code getInjector} will be removed from {@link Exec} very soon during Embulk v0.10.
     */
    @Deprecated  // https://github.com/embulk/embulk/issues/1313
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1313
    public static Injector getInjector() {
        return sessionForInside().getInjector();
    }

    @Deprecated  // https://github.com/embulk/embulk/issues/1292
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    public static org.embulk.spi.time.Timestamp getTransactionTime() {
        return org.embulk.spi.time.Timestamp.ofInstant(sessionForInside().getTransactionTimeInstant());
    }

    public static Instant getTransactionTimeInstant() {
        return sessionForInside().getTransactionTimeInstant();
    }

    @Deprecated  // @see docs/design/slf4j.md
    @SuppressWarnings("deprecation")
    public static Logger getLogger(String name) {
        return sessionForInside().getLogger(name);
    }

    @Deprecated  // @see docs/design/slf4j.md
    @SuppressWarnings("deprecation")
    public static Logger getLogger(Class<?> name) {
        return sessionForInside().getLogger(name);
    }

    public static BufferAllocator getBufferAllocator() {
        return sessionForInside().getBufferAllocator();
    }

    public static PageBuilder getPageBuilder(final BufferAllocator allocator, final Schema schema, final PageOutput output) {
        return sessionForInside().getPageBuilder(allocator, schema, output);
    }

    /**
     * Returns Embulk's {@link org.embulk.config.ModelManager}.
     *
     * <p>This method is deprecated, and it will be removed from {@link Exec} very soon during Embulk v0.10. If a plugin
     * really needs it, a plugin can technically {@link ExecInternal#getModelManager} in {@code embulk-core} instead.
     *
     * <p>But, it is strongly discouraged to keep on calling it for a long time. Embulk does not guarantee any compatibility in
     * {@code embulk-core} with plugins. If a plugin calls it, the plugin may stop working at some point of Embulk versions.
     * Do it at your own risk.
     *
     * @deprecated {@code getModelManager} will be removed from {@link Exec} very soon during Embulk v0.10.
     */
    @Deprecated  // https://github.com/embulk/embulk/issues/1304
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public static org.embulk.config.ModelManager getModelManager() {
        return sessionForInside().getModelManager();
    }

    /**
     * Registers a new Embulk plugin.
     *
     * <p>This method is deprecated, and it will be removed from {@link Exec} very soon during Embulk v0.10. If a plugin
     * really needs it, a plugin can technically {@link ExecInternal#newPlugin} in {@code embulk-core} instead.
     *
     * <p>But, it is strongly discouraged to keep on calling it for a long time. Embulk does not guarantee any compatibility in
     * {@code embulk-core} with plugins. If a plugin calls it, the plugin may stop working at some point of Embulk versions.
     * Do it at your own risk.
     *
     * @deprecated {@code newPlugin} will be removed from {@link Exec} very soon during Embulk v0.10.
     */
    @Deprecated  // https://github.com/embulk/embulk/issues/1309
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1309
    public static <T> T newPlugin(Class<T> iface, PluginType type) {
        return sessionForInside().newPlugin(iface, type);
    }

    public static TaskReport newTaskReport() {
        return sessionForInside().newTaskReport();
    }

    public static ConfigDiff newConfigDiff() {
        return sessionForInside().newConfigDiff();
    }

    public static ConfigSource newConfigSource() {
        return sessionForInside().newConfigSource();
    }

    public static TaskSource newTaskSource() {
        return sessionForInside().newTaskSource();
    }

    // TODO this method is still beta
    public static TempFileSpace getTempFileSpace() {
        return sessionForInside().getTempFileSpace();
    }

    public static boolean isPreview() {
        return sessionForInside().isPreview();
    }
}
