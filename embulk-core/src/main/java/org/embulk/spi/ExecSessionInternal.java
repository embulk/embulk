package org.embulk.spi;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.GuessExecutor;
import org.embulk.jruby.JRubyPluginSource;
import org.embulk.jruby.LazyScriptingContainerDelegate;
import org.embulk.jruby.ScriptingContainerDelegate;
import org.embulk.plugin.BuiltinPluginSource;
import org.embulk.plugin.JarPluginSource;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginClassLoaderFactoryImpl;
import org.embulk.plugin.PluginManager;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.SelfContainedPluginSource;
import org.embulk.plugin.maven.MavenPluginSource;
import org.embulk.spi.TempFileSpaceAllocator;
import org.embulk.spi.time.Instants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access inside Embulk through a transactional session.
 *
 * <p>It is internal implementation of {@link ExecSession} although some methods, such as {@code newPlugin}, are to be
 * removed from {@link ExecSession} soon during Embulk v0.10 when {@link ExecSession} moves to {@code embulk-spi} from
 * {@code embulk-core}. Even after the removal, they stay in this {@link ExecSessionInternal} to be called through
 * {@link ExecInternal}.
 */
public class ExecSessionInternal extends ExecSession {
    private static final Logger logger = LoggerFactory.getLogger(ExecSessionInternal.class);

    private static final DateTimeFormatter ISO8601_BASIC =
            DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private final EmbulkSystemProperties embulkSystemProperties;
    private final GuessExecutor guessExecutor;

    @Deprecated  // https://github.com/embulk/embulk/issues/1304
    private final org.embulk.config.ModelManager modelManager;

    private final ScriptingContainerDelegate jrubyScriptingContainerDelegate;

    private final PluginClassLoaderFactory pluginClassLoaderFactory;
    private final PluginManager pluginManager;
    private final BufferAllocator bufferAllocator;

    private final Instant transactionTime;
    private final TempFileSpace tempFileSpace;

    private final boolean preview;

    @Deprecated  // TODO: Remove it.
    private interface SessionTask extends Task {
        @Config("transaction_time")
        @ConfigDefault("null")
        Optional<String> getTransactionTime();
    }

    public static class Builder {
        private final BufferAllocator bufferAllocator;
        private final TempFileSpaceAllocator tempFileSpaceAllocator;

        private EmbulkSystemProperties embulkSystemProperties;
        private GuessExecutor guessExecutor;
        private BuiltinPluginSource.Builder builtinPluginSourceBuilder;
        private Instant transactionTime;

        @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
        private org.embulk.config.ModelManager modelManager;

        public Builder(
                final BufferAllocator bufferAllocator,
                final TempFileSpaceAllocator tempFileSpaceAllocator) {
            this.bufferAllocator = bufferAllocator;
            this.tempFileSpaceAllocator = tempFileSpaceAllocator;
            this.embulkSystemProperties = null;
            this.builtinPluginSourceBuilder = BuiltinPluginSource.builder();
            this.transactionTime = null;
            this.modelManager = null;
        }

        public Builder fromExecConfig(ConfigSource configSource) {
            final Optional<Instant> transactionTime =
                    toInstantFromString(configSource.get(String.class, "transaction_time", null));
            if (transactionTime.isPresent()) {
                this.transactionTime = transactionTime.get();
            }
            return this;
        }

        @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
        public Builder setModelManager(final org.embulk.config.ModelManager modelManager) {
            this.modelManager = modelManager;
            return this;
        }

        public Builder setGuessExecutor(final GuessExecutor guessExecutor) {
            this.guessExecutor = guessExecutor;
            return this;
        }

        public Builder registerDecoderPlugin(final String name, final Class<? extends DecoderPlugin> decoderImpl) {
            this.builtinPluginSourceBuilder.registerDecoderPlugin(name, decoderImpl);
            return this;
        }

        public Builder registerEncoderPlugin(final String name, final Class<? extends EncoderPlugin> encoderImpl) {
            this.builtinPluginSourceBuilder.registerEncoderPlugin(name, encoderImpl);
            return this;
        }

        public Builder registerExecutorPlugin(final String name, final Class<? extends ExecutorPlugin> executorImpl) {
            this.builtinPluginSourceBuilder.registerExecutorPlugin(name, executorImpl);
            return this;
        }

        public Builder registerFileInputPlugin(final String name, final Class<? extends FileInputPlugin> fileInputImpl) {
            this.builtinPluginSourceBuilder.registerFileInputPlugin(name, fileInputImpl);
            return this;
        }

        public Builder registerFileOutputPlugin(final String name, final Class<? extends FileOutputPlugin> fileOutputImpl) {
            this.builtinPluginSourceBuilder.registerFileOutputPlugin(name, fileOutputImpl);
            return this;
        }

        public Builder registerFilterPlugin(final String name, final Class<? extends FilterPlugin> filterImpl) {
            this.builtinPluginSourceBuilder.registerFilterPlugin(name, filterImpl);
            return this;
        }

        public Builder registerFormatterPlugin(final String name, final Class<? extends FormatterPlugin> formatterImpl) {
            this.builtinPluginSourceBuilder.registerFormatterPlugin(name, formatterImpl);
            return this;
        }

        public Builder registerGuessPlugin(final String name, final Class<? extends GuessPlugin> guessImpl) {
            this.builtinPluginSourceBuilder.registerGuessPlugin(name, guessImpl);
            return this;
        }

        public Builder registerInputPlugin(final String name, final Class<? extends InputPlugin> inputImpl) {
            this.builtinPluginSourceBuilder.registerInputPlugin(name, inputImpl);
            return this;
        }

        public Builder registerOutputPlugin(final String name, final Class<? extends OutputPlugin> outputImpl) {
            this.builtinPluginSourceBuilder.registerOutputPlugin(name, outputImpl);
            return this;
        }

        public Builder registerParserPlugin(final String name, final Class<? extends ParserPlugin> parserImpl) {
            this.builtinPluginSourceBuilder.registerParserPlugin(name, parserImpl);
            return this;
        }

        public Builder setEmbulkSystemProperties(final EmbulkSystemProperties embulkSystemProperties) {
            this.embulkSystemProperties = embulkSystemProperties;
            this.builtinPluginSourceBuilder.setEmbulkSystemProperties(embulkSystemProperties);
            return this;
        }

        @Deprecated  // TODO: Add setTransactionTime(Instant) if needed. But no one looks using it. May not be needed.
        @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
        public Builder setTransactionTime(final org.embulk.spi.time.Timestamp timestamp) {
            logger.warn("ExecSessionInternal.Builder#setTransactionTime is deprecated. Set it via ExecSessionInternal.Builder#fromExecConfig.");
            this.transactionTime = timestamp.getInstant();
            return this;
        }

        public ExecSessionInternal build() {
            if (transactionTime == null) {
                transactionTime = Instant.now();
            }
            if (this.embulkSystemProperties == null) {
                logger.warn("EmbulkSystemProperties is not set when building ExecSessionInternal, then set to empty. "
                            + "Use ExecSessionInternal.Builder#setEmbulkSystemProperties.");
                this.embulkSystemProperties = EmbulkSystemProperties.of(new Properties());
            }
            if (this.modelManager == null) {
                throw new IllegalStateException("ModelManager is not set in ExecSessionInternal.");
            }
            return new ExecSessionInternal(
                    this.transactionTime,
                    this.embulkSystemProperties,
                    this.bufferAllocator,
                    this.tempFileSpaceAllocator,
                    this.guessExecutor,
                    this.builtinPluginSourceBuilder.build(),
                    this.modelManager);
        }
    }

    public static Builder builderInternal(
            final BufferAllocator bufferAllocator,
            final TempFileSpaceAllocator tempFileSpaceAllocator) {
        return new Builder(bufferAllocator, tempFileSpaceAllocator);
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    private ExecSessionInternal(
            final Instant transactionTime,
            final EmbulkSystemProperties embulkSystemProperties,
            final BufferAllocator bufferAllocator,
            final TempFileSpaceAllocator tempFileSpaceAllocator,
            final GuessExecutor guessExecutor,
            final BuiltinPluginSource builtinPluginSource,
            final org.embulk.config.ModelManager modelManager) {
        this.embulkSystemProperties = embulkSystemProperties;
        this.guessExecutor = guessExecutor;
        this.modelManager = modelManager;

        this.jrubyScriptingContainerDelegate = LazyScriptingContainerDelegate.withEmbulkSpecific(
                LoggerFactory.getLogger("init"), this.embulkSystemProperties);

        this.pluginClassLoaderFactory = PluginClassLoaderFactoryImpl.of();
        this.pluginManager = PluginManager.with(
                embulkSystemProperties,
                builtinPluginSource,
                JarPluginSource.of(embulkSystemProperties),
                new MavenPluginSource(embulkSystemProperties, pluginClassLoaderFactory),
                new SelfContainedPluginSource(embulkSystemProperties, pluginClassLoaderFactory),
                new JRubyPluginSource(this.jrubyScriptingContainerDelegate, pluginClassLoaderFactory));

        this.bufferAllocator = bufferAllocator;

        this.transactionTime = transactionTime;

        this.tempFileSpace = tempFileSpaceAllocator.newSpace(ISO8601_BASIC.format(this.transactionTime));

        this.preview = false;
    }

    private ExecSessionInternal(ExecSessionInternal copy, boolean preview) {
        this.embulkSystemProperties = copy.embulkSystemProperties;
        this.guessExecutor = copy.guessExecutor;
        this.modelManager = copy.modelManager;
        this.jrubyScriptingContainerDelegate = copy.jrubyScriptingContainerDelegate;
        this.pluginClassLoaderFactory = copy.pluginClassLoaderFactory;
        this.pluginManager = copy.pluginManager;
        this.bufferAllocator = copy.bufferAllocator;

        this.transactionTime = copy.transactionTime;
        this.tempFileSpace = copy.tempFileSpace;

        this.preview = preview;
    }

    @Override
    public ExecSessionInternal forPreview() {
        return new ExecSessionInternal(this, true);
    }

    @Deprecated
    @Override
    @SuppressWarnings("deprecation")
    public ConfigSource getSessionExecConfig() {
        return newConfigSource()
                .set("transaction_time", Instants.toString(this.transactionTime));
    }

    @Deprecated
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1292
    @Override
    public org.embulk.spi.time.Timestamp getTransactionTime() {
        return org.embulk.spi.time.Timestamp.ofInstant(this.transactionTime);
    }

    @Override
    public Instant getTransactionTimeInstant() {
        return this.transactionTime;
    }

    @Override
    public String getTransactionTimeString() {
        return Instants.toString(this.transactionTime);
    }

    @Deprecated  // @see docs/design/slf4j.md
    @Override
    @SuppressWarnings("deprecation")
    public Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    @Deprecated  // @see docs/design/slf4j.md
    @Override
    @SuppressWarnings("deprecation")
    public Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    @Override
    public BufferAllocator getBufferAllocator() {
        return bufferAllocator;
    }

    @Override
    public PageBuilder getPageBuilder(final BufferAllocator allocator, final Schema schema, final PageOutput output) {
        return new PageBuilderImpl(allocator, schema, output);
    }

    @Override
    public PageReader getPageReader(final Schema schema) {
        return new PageReaderImpl(schema);
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public org.embulk.config.ModelManager getModelManager() {
        return modelManager;
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1309
    public <T> T newPlugin(Class<T> iface, PluginType type) {
        return pluginManager.newPlugin(iface, type);
    }

    @Override
    public TaskReport newTaskReport() {
        return this.modelManager.newTaskReport();
    }

    @Override
    public ConfigDiff newConfigDiff() {
        return this.modelManager.newConfigDiff();
    }

    @Override
    public ConfigSource newConfigSource() {
        return this.modelManager.newConfigSource();
    }

    @Override
    public TaskSource newTaskSource() {
        return this.modelManager.newTaskSource();
    }

    @Override
    public TempFileSpace getTempFileSpace() {
        return tempFileSpace;
    }

    @Override
    public boolean isPreview() {
        return preview;
    }

    @Override
    public void cleanup() {
        this.pluginClassLoaderFactory.clear();
        tempFileSpace.cleanup();
    }

    GuessExecutor getGuessExecutor() {
        return this.guessExecutor;
    }

    private static Optional<Instant> toInstantFromString(final String string) {
        if (string == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Instants.parseInstant(string));
        } catch (final NumberFormatException ex) {
            logger.warn("\"transaction_time\" is in an invalid format: '" + string + "'. "
                        + "The transaction time is set to the present.", ex);
            return Optional.empty();
        } catch (final IllegalStateException ex) {
            logger.error("Unexpected failure in parsing \"transaction_time\": '" + string + "'", ex);
            throw ex;
        }
    }
}
