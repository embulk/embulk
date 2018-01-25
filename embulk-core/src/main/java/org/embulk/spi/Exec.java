package org.embulk.spi;

import com.google.inject.Injector;
import java.util.concurrent.ExecutionException;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.ModelManager;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.spi.time.Timestamp;
import org.slf4j.Logger;

public class Exec {
    private static final InheritableThreadLocal<ExecSession> session = new InheritableThreadLocal<ExecSession>();

    private Exec() {}

    public static <T> T doWith(ExecSession session, ExecAction<T> action) throws ExecutionException {
        Exec.session.set(session);
        try {
            return action.run();
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        } finally {
            Exec.session.set(null);
        }
    }

    public static ExecSession session() {
        ExecSession session = Exec.session.get();
        if (session == null) {
            throw new NullPointerException("Exec is used outside of Exec.doWith");
        }
        return session;
    }

    public static Injector getInjector() {
        return session().getInjector();
    }

    public static Timestamp getTransactionTime() {
        return session().getTransactionTime();
    }

    public static Logger getLogger(String name) {
        return session().getLogger(name);
    }

    public static Logger getLogger(Class<?> name) {
        return session().getLogger(name);
    }

    public static BufferAllocator getBufferAllocator() {
        return session().getBufferAllocator();
    }

    public static ModelManager getModelManager() {
        return session().getModelManager();
    }

    public static <T> T newPlugin(Class<T> iface, PluginType type) {
        return session().newPlugin(iface, type);
    }

    public static TaskReport newTaskReport() {
        return session().newTaskReport();
    }

    // To be removed by v0.10 or earlier.
    @Deprecated  // https://github.com/embulk/embulk/issues/933
    @SuppressWarnings("deprecation")
    public static org.embulk.config.CommitReport newCommitReport() {
        return session().newCommitReport();
    }

    public static ConfigDiff newConfigDiff() {
        return session().newConfigDiff();
    }

    public static ConfigSource newConfigSource() {
        return session().newConfigSource();
    }

    public static TaskSource newTaskSource() {
        return session().newTaskSource();
    }

    // TODO this method is still beta
    public static TempFileSpace getTempFileSpace() {
        return session().getTempFileSpace();
    }

    public static boolean isPreview() {
        return session().isPreview();
    }
}
