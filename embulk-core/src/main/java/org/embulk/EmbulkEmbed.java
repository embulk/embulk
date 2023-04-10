package org.embulk;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.exec.BulkLoader;
import org.embulk.exec.ExecutionResult;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.PartialExecutionException;
import org.embulk.exec.PooledBufferAllocator;
import org.embulk.exec.PreviewExecutor;
import org.embulk.exec.PreviewResult;
import org.embulk.exec.ResumeState;
import org.embulk.exec.SimpleTempFileSpaceAllocator;
import org.embulk.exec.TransactionStage;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.ExecutorPlugin;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.GuessPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.TempFileSpaceAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbulkEmbed {
    private EmbulkEmbed(
            final LinkedHashMap<String, Class<? extends DecoderPlugin>> decoderPlugins,
            final LinkedHashMap<String, Class<? extends EncoderPlugin>> encoderPlugins,
            final LinkedHashMap<String, Class<? extends ExecutorPlugin>> executorPlugins,
            final LinkedHashMap<String, Class<? extends FileInputPlugin>> fileInputPlugins,
            final LinkedHashMap<String, Class<? extends FileOutputPlugin>> fileOutputPlugins,
            final LinkedHashMap<String, Class<? extends FilterPlugin>> filterPlugins,
            final LinkedHashMap<String, Class<? extends FormatterPlugin>> formatterPlugins,
            final LinkedHashMap<String, Class<? extends GuessPlugin>> guessPlugins,
            final LinkedHashMap<String, Class<? extends InputPlugin>> inputPlugins,
            final LinkedHashMap<String, Class<? extends OutputPlugin>> outputPlugins,
            final LinkedHashMap<String, Class<? extends ParserPlugin>> parserPlugins,
            final EmbulkSystemProperties embulkSystemProperties,
            final BufferAllocator bufferAllocator,
            final TempFileSpaceAllocator tempFileSpaceAllocator,
            final BulkLoader alternativeBulkLoader) {
        this.decoderPlugins = Collections.unmodifiableMap(decoderPlugins);
        this.encoderPlugins = Collections.unmodifiableMap(encoderPlugins);
        this.executorPlugins = Collections.unmodifiableMap(executorPlugins);
        this.fileInputPlugins = Collections.unmodifiableMap(fileInputPlugins);
        this.fileOutputPlugins = Collections.unmodifiableMap(fileOutputPlugins);
        this.filterPlugins = Collections.unmodifiableMap(filterPlugins);
        this.formatterPlugins = Collections.unmodifiableMap(formatterPlugins);
        this.guessPlugins = Collections.unmodifiableMap(guessPlugins);
        this.inputPlugins = Collections.unmodifiableMap(inputPlugins);
        this.outputPlugins = Collections.unmodifiableMap(outputPlugins);
        this.parserPlugins = Collections.unmodifiableMap(parserPlugins);
        this.embulkSystemProperties = embulkSystemProperties;
        this.bufferAllocator = bufferAllocator;
        this.tempFileSpaceAllocator = tempFileSpaceAllocator;

        if (alternativeBulkLoader == null) {
            this.bulkLoader = new BulkLoader(embulkSystemProperties);
        } else {
            this.bulkLoader = alternativeBulkLoader;
        }
        this.guessExecutor = new GuessExecutor(embulkSystemProperties);
        this.previewExecutor = new PreviewExecutor(embulkSystemProperties);

        this.modelManager = createModelManager();
    }

    public static class Bootstrap {
        public Bootstrap() {
            this.decoderPlugins = new LinkedHashMap<>();
            this.encoderPlugins = new LinkedHashMap<>();
            this.executorPlugins = new LinkedHashMap<>();
            this.fileInputPlugins = new LinkedHashMap<>();
            this.fileOutputPlugins = new LinkedHashMap<>();
            this.filterPlugins = new LinkedHashMap<>();
            this.formatterPlugins = new LinkedHashMap<>();
            this.guessPlugins = new LinkedHashMap<>();
            this.inputPlugins = new LinkedHashMap<>();
            this.outputPlugins = new LinkedHashMap<>();
            this.parserPlugins = new LinkedHashMap<>();
            this.embulkSystemPropertiesBuilt = new Properties();
            this.alternativeBulkLoader = null;
            this.started = false;
        }

        public Bootstrap builtinDecoderPlugin(final String name, final Class<? extends DecoderPlugin> decoderImpl) {
            this.decoderPlugins.put(name, decoderImpl);
            return this;
        }

        public Bootstrap builtinEncoderPlugin(final String name, final Class<? extends EncoderPlugin> encoderImpl) {
            this.encoderPlugins.put(name, encoderImpl);
            return this;
        }

        public Bootstrap builtinExecutorPlugin(final String name, final Class<? extends ExecutorPlugin> executorImpl) {
            this.executorPlugins.put(name, executorImpl);
            return this;
        }

        public Bootstrap builtinFileInputPlugin(final String name, final Class<? extends FileInputPlugin> fileInputImpl) {
            this.fileInputPlugins.put(name, fileInputImpl);
            return this;
        }

        public Bootstrap builtinFileOutputPlugin(final String name, final Class<? extends FileOutputPlugin> fileOutputImpl) {
            this.fileOutputPlugins.put(name, fileOutputImpl);
            return this;
        }

        public Bootstrap builtinFilterPlugin(final String name, final Class<? extends FilterPlugin> filterImpl) {
            this.filterPlugins.put(name, filterImpl);
            return this;
        }

        public Bootstrap builtinFormatterPlugin(final String name, final Class<? extends FormatterPlugin> formatterImpl) {
            this.formatterPlugins.put(name, formatterImpl);
            return this;
        }

        public Bootstrap builtinGuessPlugin(final String name, final Class<? extends GuessPlugin> guessImpl) {
            this.guessPlugins.put(name, guessImpl);
            return this;
        }

        public Bootstrap builtinInputPlugin(final String name, final Class<? extends InputPlugin> inputImpl) {
            this.inputPlugins.put(name, inputImpl);
            return this;
        }

        public Bootstrap builtinOutputPlugin(final String name, final Class<? extends OutputPlugin> outputImpl) {
            this.outputPlugins.put(name, outputImpl);
            return this;
        }

        public Bootstrap builtinParserPlugin(final String name, final Class<? extends ParserPlugin> parserImpl) {
            this.parserPlugins.put(name, parserImpl);
            return this;
        }

        public Bootstrap setEmbulkSystemProperties(final Properties propertiesGiven) {
            this.embulkSystemPropertiesBuilt = propertiesGiven;
            return this;
        }

        /**
         * Sets an alternative {@code BulkLoader} instance mainly for testing-purpose.
         */
        public Bootstrap setAlternativeBulkLoader(final BulkLoader alternativeBulkLoader) {
            this.alternativeBulkLoader = alternativeBulkLoader;
            return this;
        }

        public EmbulkEmbed initialize() {
            if (this.started) {
                throw new IllegalStateException("System already initialized");
            }
            this.started = true;

            final EmbulkSystemProperties embulkSystemProperties = EmbulkSystemProperties.of(this.embulkSystemPropertiesBuilt);
            final BufferAllocator bufferAllocator = createBufferAllocatorFromSystemConfig(embulkSystemProperties);
            final TempFileSpaceAllocator tempFileSpaceAllocator = new SimpleTempFileSpaceAllocator();

            return new EmbulkEmbed(
                    decoderPlugins,
                    encoderPlugins,
                    executorPlugins,
                    fileInputPlugins,
                    fileOutputPlugins,
                    filterPlugins,
                    formatterPlugins,
                    guessPlugins,
                    inputPlugins,
                    outputPlugins,
                    parserPlugins,
                    embulkSystemProperties,
                    bufferAllocator,
                    tempFileSpaceAllocator,
                    alternativeBulkLoader);
        }

        @Deprecated
        public EmbulkEmbed initializeCloseable() {
            return this.initialize();
        }

        // We are trying to represent the "system config" in java.util.Properties, instead of ConfigSource.
        // TODO: Make this java.util.Properties use as system config. See: https://github.com/embulk/embulk/issues/1159
        private Properties embulkSystemPropertiesBuilt;

        private BulkLoader alternativeBulkLoader;

        private final LinkedHashMap<String, Class<? extends DecoderPlugin>> decoderPlugins;
        private final LinkedHashMap<String, Class<? extends EncoderPlugin>> encoderPlugins;
        private final LinkedHashMap<String, Class<? extends ExecutorPlugin>> executorPlugins;
        private final LinkedHashMap<String, Class<? extends FileInputPlugin>> fileInputPlugins;
        private final LinkedHashMap<String, Class<? extends FileOutputPlugin>> fileOutputPlugins;
        private final LinkedHashMap<String, Class<? extends FilterPlugin>> filterPlugins;
        private final LinkedHashMap<String, Class<? extends FormatterPlugin>> formatterPlugins;
        private final LinkedHashMap<String, Class<? extends GuessPlugin>> guessPlugins;
        private final LinkedHashMap<String, Class<? extends InputPlugin>> inputPlugins;
        private final LinkedHashMap<String, Class<? extends OutputPlugin>> outputPlugins;
        private final LinkedHashMap<String, Class<? extends ParserPlugin>> parserPlugins;

        private boolean started;
    }

    @Deprecated  // https://github.com/embulk/embulk/issues/1304
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public org.embulk.config.ModelManager getModelManager() {
        return this.modelManager;
    }

    public Object dumpObjectFromDataSource(final DataSource modelObject) {
        return modelManager.readObject(Object.class, this.modelManager.writeObject(modelObject));
    }

    public Object dumpObjectFromResumeState(final ResumeState modelObject) {
        return modelManager.readObject(Object.class, this.modelManager.writeObject(modelObject));
    }

    public BufferAllocator getBufferAllocator() {
        return this.bufferAllocator;
    }

    public ConfigLoader newConfigLoader() {
        return new ConfigLoader(this.modelManager);
    }

    public ConfigDiff guess(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSessionInternal exec = this.newExecSessionInternal(config.deepCopy().getNestedOrGetEmpty("exec"));
        try {
            return this.guessExecutor.guess(exec, config);
        } finally {
            exec.cleanup();
        }
    }

    public PreviewResult preview(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSessionInternal exec = this.newExecSessionInternal(config.deepCopy().getNestedOrGetEmpty("exec"));
        try {
            return this.previewExecutor.preview(exec, config);
        } finally {
            exec.cleanup();
        }
    }

    public ExecutionResult run(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSessionInternal exec = this.newExecSessionInternal(config.deepCopy().getNestedOrGetEmpty("exec"));
        try {
            return this.bulkLoader.run(exec, config);
        } catch (final PartialExecutionException partial) {
            final ExecSessionInternal cleanupExec =
                    this.newExecSessionInternal(partial.getResumeState().getExecSessionConfigSource());
            try {
                this.bulkLoader.cleanup(cleanupExec, config, partial.getResumeState());
            } catch (final Throwable ex) {
                partial.addSuppressed(ex);
            }
            throw partial;
        } finally {
            try {
                exec.cleanup();
            } catch (final Exception ex) {
                // TODO add this exception to ExecutionResult.getIgnoredExceptions
                // or partial.addSuppressed
                ex.printStackTrace(System.err);
            }
        }
    }

    public ResumableResult runResumable(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSessionInternal exec = this.newExecSessionInternal(config.deepCopy().getNestedOrGetEmpty("exec"));
        try {
            final ExecutionResult result;
            try {
                result = this.bulkLoader.run(exec, config);
            } catch (PartialExecutionException partial) {
                return new ResumableResult(partial);
            }
            return new ResumableResult(result);
        } finally {
            try {
                exec.cleanup();
            } catch (final Exception ex) {
                // TODO add this exception to ExecutionResult.getIgnoredExceptions
                // or partial.addSuppressed
                ex.printStackTrace(System.err);
            }
        }
    }

    private ExecSessionInternal newExecSessionInternal(final ConfigSource execConfig) {
        final ExecSessionInternal.Builder builder = ExecSessionInternal.builderInternal(
                this.bufferAllocator, this.tempFileSpaceAllocator);
        for (final Map.Entry<String, Class<? extends DecoderPlugin>> decoderPlugin : this.decoderPlugins.entrySet()) {
            builder.registerDecoderPlugin(decoderPlugin.getKey(), decoderPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends EncoderPlugin>> encoderPlugin : this.encoderPlugins.entrySet()) {
            builder.registerEncoderPlugin(encoderPlugin.getKey(), encoderPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends ExecutorPlugin>> executorPlugin : this.executorPlugins.entrySet()) {
            builder.registerExecutorPlugin(executorPlugin.getKey(), executorPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends FileInputPlugin>> fileInputPlugin : this.fileInputPlugins.entrySet()) {
            builder.registerFileInputPlugin(fileInputPlugin.getKey(), fileInputPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends FileOutputPlugin>> fileOutputPlugin : this.fileOutputPlugins.entrySet()) {
            builder.registerFileOutputPlugin(fileOutputPlugin.getKey(), fileOutputPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends FilterPlugin>> filterPlugin : this.filterPlugins.entrySet()) {
            builder.registerFilterPlugin(filterPlugin.getKey(), filterPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends FormatterPlugin>> formatterPlugin : this.formatterPlugins.entrySet()) {
            builder.registerFormatterPlugin(formatterPlugin.getKey(), formatterPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends GuessPlugin>> guessPlugin : this.guessPlugins.entrySet()) {
            builder.registerGuessPlugin(guessPlugin.getKey(), guessPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends InputPlugin>> inputPlugin : this.inputPlugins.entrySet()) {
            builder.registerInputPlugin(inputPlugin.getKey(), inputPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends OutputPlugin>> outputPlugin : this.outputPlugins.entrySet()) {
            builder.registerOutputPlugin(outputPlugin.getKey(), outputPlugin.getValue());
        }
        for (final Map.Entry<String, Class<? extends ParserPlugin>> parserPlugin : this.parserPlugins.entrySet()) {
            builder.registerParserPlugin(parserPlugin.getKey(), parserPlugin.getValue());
        }
        return builder
                .setModelManager(this.modelManager)
                .setEmbulkSystemProperties(this.embulkSystemProperties)
                .setGuessExecutor(this.guessExecutor)
                .setParentFirstPackages(PARENT_FIRST_PACKAGES)
                .setParentFirstResources(PARENT_FIRST_RESOURCES)
                .fromExecConfig(execConfig)
                .build();
    }

    @SuppressWarnings("deprecation") // https://github.com/embulk/embulk/issues/1301
    public ResumeStateAction resumeState(final ConfigSource config, final ConfigSource resumeStateConfig) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ResumeState resumeState = resumeStateConfig.loadConfig(ResumeState.class);
        return new ResumeStateAction(config, resumeState);
    }

    public static class ResumableResult {
        public ResumableResult(final PartialExecutionException partialExecutionException) {
            this.successfulResult = null;
            if (partialExecutionException == null) {
                throw new NullPointerException();
            }
            this.partialExecutionException = partialExecutionException;
        }

        public ResumableResult(final ExecutionResult successfulResult) {
            if (successfulResult == null) {
                throw new NullPointerException();
            }
            this.successfulResult = successfulResult;
            this.partialExecutionException = null;
        }

        public boolean isSuccessful() {
            return this.successfulResult != null;
        }

        public ExecutionResult getSuccessfulResult() {
            if (this.successfulResult == null) {
                throw new IllegalStateException();
            }
            return this.successfulResult;
        }

        public Throwable getCause() {
            if (this.partialExecutionException == null) {
                throw new IllegalStateException();
            }
            return this.partialExecutionException.getCause();
        }

        public ResumeState getResumeState() {
            if (this.partialExecutionException == null) {
                throw new IllegalStateException();
            }
            return this.partialExecutionException.getResumeState();
        }

        public TransactionStage getTransactionStage() {
            return this.partialExecutionException.getTransactionStage();
        }

        private final ExecutionResult successfulResult;
        private final PartialExecutionException partialExecutionException;
    }

    public class ResumeStateAction {
        public ResumeStateAction(final ConfigSource config, final ResumeState resumeState) {
            this.config = config;
            this.resumeState = resumeState;
        }

        public ResumableResult resume() {
            final ExecSessionInternal exec = newExecSessionInternal(this.resumeState.getExecSessionConfigSource());
            final ExecutionResult result;
            try {
                result = bulkLoader.resume(exec, config, resumeState);
            } catch (PartialExecutionException partial) {
                return new ResumableResult(partial);
            }
            return new ResumableResult(result);
        }

        public void cleanup() {
            final ExecSessionInternal exec = newExecSessionInternal(this.resumeState.getExecSessionConfigSource());
            bulkLoader.cleanup(exec, config, resumeState);
        }

        private final ConfigSource config;
        private final ResumeState resumeState;
    }

    public void destroy() {
        throw new UnsupportedOperationException(
                "EmbulkEmbed#destroy() is no longer supported as JSR-250 lifecycle annotations are unsupported. "
                + "See https://github.com/embulk/embulk/issues/1047 for the details.");
    }

    private static Set<String> readPropertyKeys(final String name) {
        try (final InputStream in = EmbulkEmbed.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new NullPointerException(
                        String.format("Resource '%s' is not found in classpath. Jar file or classloader is broken.", name));
            }
            final Properties properties = new Properties();
            properties.load(in);
            return Collections.unmodifiableSet(properties.stringPropertyNames());
        } catch (final IOException ex) {
            logger.error("Failed to read a resource :" + name + ". Ingored.", ex);
            return Collections.unmodifiableSet(new HashSet<>());
        }
    }

    private static BufferAllocator createBufferAllocatorFromSystemConfig(final EmbulkSystemProperties embulkSystemProperties) {
        final String byteSizeRepresentation = embulkSystemProperties.getProperty("page_size");
        if (byteSizeRepresentation == null) {
            return PooledBufferAllocator.create();
        } else {
            final int byteSize = parseByteSizeRepresentation(byteSizeRepresentation);
            return PooledBufferAllocator.create(byteSize);
        }
    }

    private static int parseByteSizeRepresentation(final String byteSizeRepresentation) {
        if (byteSizeRepresentation == null) {  // Should not happen.
            throw new NullPointerException("size is null");
        }
        if (byteSizeRepresentation.isEmpty()) {
            throw new IllegalArgumentException("size is empty");
        }

        final Matcher matcher = BYTE_SIZE_PATTERN.matcher(byteSizeRepresentation);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid byte size string '" + byteSizeRepresentation + "'");
        }

        final String numberPart = matcher.group(1);
        final String unitPart = matcher.group(2);

        final BigDecimal number = new BigDecimal(numberPart);  // NumberFormatException extends IllegalArgumentException.

        if (unitPart.isEmpty()) {
            return number.intValue();
        }

        switch (unitPart.toUpperCase(Locale.ENGLISH)) {
            case "B":
                return number.intValue();
            case "KB":
                return number.multiply(KILO).intValue();
            case "MB":
                return number.multiply(MEGA).intValue();
            case "GB":
                return number.multiply(GIGA).intValue();
            case "TB":
                return number.multiply(TERA).intValue();
            case "PB":
                return number.multiply(PETA).intValue();
            default:
                throw new IllegalArgumentException("Unknown unit '" + unitPart + "'");
        }
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    private static org.embulk.config.ModelManager createModelManager() {
        return new org.embulk.config.ModelManager();
    }

    // TODO: Remove them finally.
    static final Set<String> PARENT_FIRST_PACKAGES = readPropertyKeys("/embulk/parent_first_packages.properties");
    static final Set<String> PARENT_FIRST_RESOURCES = readPropertyKeys("/embulk/parent_first_resources.properties");

    private static final Pattern BYTE_SIZE_PATTERN = Pattern.compile("\\A(\\d+(?:\\.\\d+)?)\\s?([a-zA-Z]*)\\z");

    private static final BigDecimal KILO = new BigDecimal(1L << 10);  // 1_024
    private static final BigDecimal MEGA = new BigDecimal(1L << 20);  // 1_048_576
    private static final BigDecimal GIGA = new BigDecimal(1L << 30);  // 1_073_741_824
    private static final BigDecimal TERA = new BigDecimal(1L << 40);  // 1_099_511_627_776
    private static final BigDecimal PETA = new BigDecimal(1L << 50);  // 1_125_899_906_842_624

    private static final Logger logger = LoggerFactory.getLogger(EmbulkEmbed.class);

    private final EmbulkSystemProperties embulkSystemProperties;
    private final BufferAllocator bufferAllocator;
    private final TempFileSpaceAllocator tempFileSpaceAllocator;

    private final Map<String, Class<? extends DecoderPlugin>> decoderPlugins;
    private final Map<String, Class<? extends EncoderPlugin>> encoderPlugins;
    private final Map<String, Class<? extends ExecutorPlugin>> executorPlugins;
    private final Map<String, Class<? extends FileInputPlugin>> fileInputPlugins;
    private final Map<String, Class<? extends FileOutputPlugin>> fileOutputPlugins;
    private final Map<String, Class<? extends FilterPlugin>> filterPlugins;
    private final Map<String, Class<? extends FormatterPlugin>> formatterPlugins;
    private final Map<String, Class<? extends GuessPlugin>> guessPlugins;
    private final Map<String, Class<? extends InputPlugin>> inputPlugins;
    private final Map<String, Class<? extends OutputPlugin>> outputPlugins;
    private final Map<String, Class<? extends ParserPlugin>> parserPlugins;

    private final BulkLoader bulkLoader;
    private final GuessExecutor guessExecutor;
    private final PreviewExecutor previewExecutor;

    @Deprecated  // https://github.com/embulk/embulk/issues/1304
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    private final org.embulk.config.ModelManager modelManager;
}
