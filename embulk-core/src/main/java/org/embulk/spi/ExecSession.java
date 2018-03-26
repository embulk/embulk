package org.embulk.spi;

import com.google.common.base.Optional;
import com.google.inject.Injector;
import java.util.Map;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSourceImpl;
import org.embulk.config.ModelManager;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.TempFileAllocator;
import org.embulk.plugin.PluginManager;
import org.embulk.plugin.PluginType;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class ExecSession {
    private final Injector injector;
    private final ILoggerFactory loggerFactory;
    private final ModelManager modelManager;
    private final PluginManager pluginManager;
    private final BufferAllocator bufferAllocator;

    private final Timestamp transactionTime;
    private final TempFileSpace tempFileSpace;

    private final boolean preview;

    private Map<Reporter.Channel, Reporter> reporters;

    @Deprecated
    public interface SessionTask extends Task {
        @Config("transaction_time")
        @ConfigDefault("null")
        Optional<Timestamp> getTransactionTime();
    }

    public static class Builder {
        private final Injector injector;
        private ILoggerFactory loggerFactory;
        private Timestamp transactionTime;

        public Builder(Injector injector) {
            this.injector = injector;
        }

        public Builder fromExecConfig(ConfigSource configSource) {
            this.transactionTime = configSource.get(Timestamp.class, "transaction_time", null);
            return this;
        }

        public Builder setLoggerFactory(ILoggerFactory loggerFactory) {
            this.loggerFactory = loggerFactory;
            return this;
        }

        public Builder setTransactionTime(Timestamp timestamp) {
            this.transactionTime = timestamp;
            return this;
        }

        public ExecSession build() {
            if (transactionTime == null) {
                transactionTime = Timestamp.ofEpochMilli(System.currentTimeMillis());  // TODO get nanoseconds for default
            }
            return new ExecSession(injector, transactionTime, Optional.fromNullable(loggerFactory));
        }
    }

    public static Builder builder(Injector injector) {
        return new Builder(injector);
    }

    @Deprecated
    public ExecSession(Injector injector, ConfigSource configSource) {
        this(injector,
             configSource.loadConfig(SessionTask.class).getTransactionTime().or(
                     Timestamp.ofEpochMilli(System.currentTimeMillis())),  // TODO get nanoseconds for default
             null);
    }

    private ExecSession(Injector injector, Timestamp transactionTime, Optional<ILoggerFactory> loggerFactory) {
        this.injector = injector;
        this.loggerFactory = loggerFactory.or(injector.getInstance(ILoggerFactory.class));
        this.modelManager = injector.getInstance(ModelManager.class);
        this.pluginManager = injector.getInstance(PluginManager.class);
        this.bufferAllocator = injector.getInstance(BufferAllocator.class);

        this.transactionTime = transactionTime;

        TempFileAllocator tempFileAllocator = injector.getInstance(TempFileAllocator.class);
        this.tempFileSpace = tempFileAllocator.newSpace(transactionTime.toString());

        this.preview = false;
    }

    private ExecSession(ExecSession copy, boolean preview) {
        this.injector = copy.injector;
        this.loggerFactory = copy.loggerFactory;
        this.modelManager = copy.modelManager;
        this.pluginManager = copy.pluginManager;
        this.bufferAllocator = copy.bufferAllocator;

        this.transactionTime = copy.transactionTime;
        this.tempFileSpace = copy.tempFileSpace;

        this.preview = preview;
    }

    public ExecSession forPreview() {
        return new ExecSession(this, true);
    }

    public ConfigSource getSessionExecConfig() {
        return newConfigSource()
                .set("transaction_time", transactionTime);
    }

    public Injector getInjector() {
        return injector;
    }

    public Timestamp getTransactionTime() {
        return transactionTime;
    }

    public Logger getLogger(String name) {
        return loggerFactory.getLogger(name);
    }

    public Logger getLogger(Class<?> name) {
        return loggerFactory.getLogger(name.getName());
    }

    public BufferAllocator getBufferAllocator() {
        return bufferAllocator;
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    public <T> T newPlugin(Class<T> iface, PluginType type) {
        return pluginManager.newPlugin(iface, type);
    }

    public TaskReport newTaskReport() {
        return new DataSourceImpl(modelManager);
    }

    // To be removed by v0.10 or earlier.
    @Deprecated  // https://github.com/embulk/embulk/issues/933
    @SuppressWarnings("deprecation")
    public org.embulk.config.CommitReport newCommitReport() {
        return new DataSourceImpl(modelManager);
    }

    public ConfigDiff newConfigDiff() {
        return new DataSourceImpl(modelManager);
    }

    public ConfigSource newConfigSource() {
        return new DataSourceImpl(modelManager);
    }

    public TaskSource newTaskSource() {
        return new DataSourceImpl(modelManager);
    }

    // To be removed by v0.10 or earlier.
    @Deprecated  // https://github.com/embulk/embulk/issues/936
    @SuppressWarnings("deprecation")
    public TimestampFormatter newTimestampFormatter(String format, org.joda.time.DateTimeZone timezone) {
        return new TimestampFormatter(format, timezone);
    }

    public void setReporters(final Map<Reporter.Channel, Reporter> reporters) {
        this.reporters = reporters;
    }

    public Reporter getReporter(Reporter.Channel channel) {
        return this.reporters.get(channel); // getOrDefault?
    }

    public TempFileSpace getTempFileSpace() {
        return tempFileSpace;
    }

    public boolean isPreview() {
        return preview;
    }

    public void cleanup() {
        for (final Reporter.Channel channel : Reporter.Channel.values()) {
            final AbstractReporterImpl reporter = (AbstractReporterImpl) reporters.get(channel);
            try {
                reporter.cleanup();
            } catch (Exception ex) {
                // ignore exception
            }
        }

        tempFileSpace.cleanup();
    }
}
