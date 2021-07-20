package org.embulk.spi;

import com.google.inject.Injector;
import java.util.concurrent.ExecutionException;
import org.embulk.exec.GuessExecutor;
import org.embulk.plugin.PluginType;

/**
 * Provides static access inside Embulk.
 *
 * <p>It complements methods deprecated/removed from {@link Exec}. It is used from {@code embulk-core} internally.
 *
 * <p>A plugin can technically call them via {@link ExecInternal} in {@code embulk-core} though it is strongly discouraged.
 * Embulk does not guarantee any compatibility in {@code embulk-core} with plugins. The plugin may easily stop working at
 * some point of Embulk versions. Do it at your own risk.
 */
public class ExecInternal {
    private ExecInternal() {}

    public static <T> T doWith(
            final ExecSessionInternal sessionInternal,
            final ExecAction<T> action)
            throws ExecutionException {
        ExecInternal.sessionInternal.set(sessionInternal);
        Exec.setThreadLocalSession(sessionInternal);
        try {
            return action.run();
        } catch (final Exception ex) {
            throw new ExecutionException(ex);
        } finally {
            Exec.setThreadLocalSession(null);
            ExecInternal.sessionInternal.set(null);
        }
    }

    public static ExecSessionInternal sessionInternal() {
        final ExecSessionInternal sessionInternal = ExecInternal.sessionInternal.get();
        if (sessionInternal == null) {
            throw new NullPointerException("Exec is used outside of ExecInternal.doWith");
        }
        return sessionInternal;
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1313
    public static Injector getInjector() {
        return sessionInternal().getInjector();
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public static org.embulk.config.ModelManager getModelManager() {
        return sessionInternal().getModelManager();
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1309
    public static <T> T newPlugin(final Class<T> iface, final PluginType type) {
        return sessionInternal().newPlugin(iface, type);
    }

    static GuessExecutor getGuessExecutor() {
        return sessionInternal().getGuessExecutor();
    }

    private static final InheritableThreadLocal<ExecSessionInternal> sessionInternal =
            new InheritableThreadLocal<ExecSessionInternal>();
}
