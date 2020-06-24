package org.embulk.spi;

import com.google.inject.Injector;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.embulk.EmbulkSystemProperties;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSourceImpl;
import org.embulk.config.ModelManager;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.jruby.JRubyPluginSource;
import org.embulk.jruby.ScriptingContainerDelegate;
import org.embulk.plugin.InjectedPluginSource;
import org.embulk.plugin.PluginClassLoaderFactory;
import org.embulk.plugin.PluginClassLoaderFactoryImpl;
import org.embulk.plugin.PluginManager;
import org.embulk.plugin.PluginType;
import org.embulk.plugin.maven.MavenPluginSource;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecSession {
    private static final Logger logger = LoggerFactory.getLogger(ExecSession.class);

    private static final DateTimeFormatter ISO8601_BASIC =
            DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'", Locale.ENGLISH).withZone(ZoneOffset.UTC);
    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(?:\\.(\\d{1,9}))? (?:UTC|\\+?00\\:?00)");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER_MILLISECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    private final Injector injector;
    private final EmbulkSystemProperties embulkSystemProperties;

    private final ModelManager modelManager;
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
        private final Injector injector;
        private EmbulkSystemProperties embulkSystemProperties;
        private Set<String> parentFirstPackages;
        private Set<String> parentFirstResources;
        private Instant transactionTime;

        public Builder(Injector injector) {
            this.injector = injector;
            this.embulkSystemProperties = null;
            this.parentFirstPackages = null;
            this.parentFirstResources = null;
            this.transactionTime = null;
        }

        public Builder fromExecConfig(ConfigSource configSource) {
            final Optional<Instant> transactionTime =
                    toInstantFromString(configSource.get(String.class, "transaction_time", null));
            if (transactionTime.isPresent()) {
                this.transactionTime = transactionTime.get();
            }
            return this;
        }

        public Builder setEmbulkSystemProperties(final EmbulkSystemProperties embulkSystemProperties) {
            this.embulkSystemProperties = embulkSystemProperties;
            return this;
        }

        public Builder setParentFirstPackages(final Set<String> parentFirstPackages) {
            this.parentFirstPackages = Collections.unmodifiableSet(new HashSet<>(parentFirstPackages));
            return this;
        }

        public Builder setParentFirstResources(final Set<String> parentFirstResources) {
            this.parentFirstResources = Collections.unmodifiableSet(new HashSet<>(parentFirstResources));
            return this;
        }

        @Deprecated  // TODO: Add setTransactionTime(Instant) if needed. But no one looks using it. May not be needed.
        public Builder setTransactionTime(Timestamp timestamp) {
            logger.warn("ExecSession.Builder#setTransactionTime is deprecated. Set it via ExecSession.Builder#fromExecConfig.");
            this.transactionTime = timestamp.getInstant();
            return this;
        }

        public ExecSession build() {
            if (transactionTime == null) {
                transactionTime = Instant.now();
            }
            if (this.embulkSystemProperties == null) {
                logger.warn("EmbulkSystemProperties is not set when building ExecSession. "
                            + "Use ExecSession.Builder#setEmbulkSystemProperties.");
                this.embulkSystemProperties = this.injector.getInstance(EmbulkSystemProperties.class);
            }
            return new ExecSession(
                    this.injector,
                    this.transactionTime,
                    this.embulkSystemProperties,
                    this.parentFirstPackages,
                    this.parentFirstResources);
        }
    }

    public static Builder builder(Injector injector) {
        return new Builder(injector);
    }

    @Deprecated
    @SuppressWarnings("deprecation")  // For the use of SessionTask.
    public ExecSession(Injector injector, ConfigSource configSource) {
        this(injector,
             configSource.loadConfig(SessionTask.class).getTransactionTime().flatMap(ExecSession::toInstantFromString).orElse(
                     Instant.now()),
             injector.getInstance(EmbulkSystemProperties.class),
             null,
             null);
        logger.warn("Constructing with ExecSession(Injector, ConfigSource) is deprecated. Use ExecSession.Builder instead.");
    }

    private ExecSession(
            final Injector injector,
            final Instant transactionTime,
            final EmbulkSystemProperties embulkSystemProperties,
            final Set<String> parentFirstPackages,
            final Set<String> parentFirstResources) {
        if (parentFirstPackages == null) {
            logger.warn("Parent-first packages are not set when building ExecSession. "
                        + "Use ExecSession.Builder#setParentFirstPackages.");
        }
        if (parentFirstResources == null) {
            logger.warn("Parent-first resources are not set when building ExecSession. "
                        + "Use ExecSession.Builder#setParentFirstResources.");
        }

        this.injector = injector;
        this.embulkSystemProperties = embulkSystemProperties;
        this.modelManager = injector.getInstance(ModelManager.class);

        this.pluginClassLoaderFactory = PluginClassLoaderFactoryImpl.of(
                (parentFirstPackages != null) ? parentFirstPackages : Collections.unmodifiableSet(new HashSet<>()),
                (parentFirstResources != null) ? parentFirstResources : Collections.unmodifiableSet(new HashSet<>()));
        this.pluginManager = PluginManager.with(
                new InjectedPluginSource(injector),
                new MavenPluginSource(injector, embulkSystemProperties, pluginClassLoaderFactory),
                new JRubyPluginSource(injector.getInstance(ScriptingContainerDelegate.class), pluginClassLoaderFactory));

        this.bufferAllocator = injector.getInstance(BufferAllocator.class);

        this.transactionTime = transactionTime;

        final TempFileSpaceAllocator tempFileSpaceAllocator = injector.getInstance(TempFileSpaceAllocator.class);
        this.tempFileSpace = tempFileSpaceAllocator.newSpace(ISO8601_BASIC.format(this.transactionTime));

        this.preview = false;
    }

    private ExecSession(ExecSession copy, boolean preview) {
        this.injector = copy.injector;
        this.embulkSystemProperties = copy.embulkSystemProperties;
        this.modelManager = copy.modelManager;
        this.pluginClassLoaderFactory = copy.pluginClassLoaderFactory;
        this.pluginManager = copy.pluginManager;
        this.bufferAllocator = copy.bufferAllocator;

        this.transactionTime = copy.transactionTime;
        this.tempFileSpace = copy.tempFileSpace;

        this.preview = preview;
    }

    public ExecSession forPreview() {
        return new ExecSession(this, true);
    }

    @Deprecated
    public ConfigSource getSessionExecConfig() {
        return newConfigSource()
                .set("transaction_time", toStringFromInstant(this.transactionTime));
    }

    public Injector getInjector() {
        return injector;
    }

    @Deprecated
    public Timestamp getTransactionTime() {
        return Timestamp.ofInstant(this.transactionTime);
    }

    public Instant getTransactionTimeInstant() {
        return this.transactionTime;
    }

    public String getTransactionTimeString() {
        return toStringFromInstant(this.transactionTime);
    }

    @Deprecated  // @see docs/design/slf4j.md
    public Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    @Deprecated  // @see docs/design/slf4j.md
    public Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
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

    public TempFileSpace getTempFileSpace() {
        return tempFileSpace;
    }

    public boolean isPreview() {
        return preview;
    }

    public void cleanup() {
        this.pluginClassLoaderFactory.clear();
        tempFileSpace.cleanup();
    }

    // They are to be tested with generated ExecSession, but generating ExecSession is hard
    // because it needs a lot of dependencies including Guice Injector and ModelManager.

    static Optional<Instant> toInstantFromStringForTesting(final String string) {
        return toInstantFromString(string);
    }

    static String toStringFromInstantForTesting(final Instant instant) {
        return toStringFromInstant(instant);
    }

    private static Optional<Instant> toInstantFromString(final String string) {
        if (string == null) {
            return Optional.empty();
        }

        final Matcher matcher = TIMESTAMP_PATTERN.matcher(string);
        if (!matcher.matches()) {
            logger.warn("\"transaction_time\" is in an invalid format: '{}'. The transaction time is set to the present.", string);
            return Optional.empty();
        }

        try {
            final long epochSecond = LocalDateTime.parse(matcher.group(1), TIMESTAMP_FORMATTER).toEpochSecond(ZoneOffset.UTC);

            final String fraction = matcher.group(2);
            final int nanoAdjustment;
            if (fraction == null) {
                nanoAdjustment = 0;
            } else {
                nanoAdjustment = Integer.parseInt(fraction) * (int) Math.pow(10, 9 - fraction.length());
            }
            return Optional.of(Instant.ofEpochSecond(epochSecond, nanoAdjustment));
        } catch (final Exception ex) {
            logger.error("\"transaction_time\" cannot be parsed unexpectedly. The transaction time is set to the present.", ex);
            return Optional.empty();
        }
    }

    private static String toStringFromInstant(final Instant instant) {
        final int nano = instant.getNano();
        if (nano == 0) {
            return TIMESTAMP_FORMATTER.format(instant) + " UTC";
        } else if (nano % 1000000 == 0) {
            return TIMESTAMP_FORMATTER_MILLISECONDS.format(instant) + " UTC";
        } else {
            final StringBuilder builder = new StringBuilder();
            TIMESTAMP_FORMATTER.formatTo(instant, builder);
            builder.append(".");

            final String digits;
            final int zeroDigits;
            if (nano % 1000 == 0) {
                digits = Integer.toString(nano / 1000);
                zeroDigits = 6 - digits.length();
            } else {
                digits = Integer.toString(nano);
                zeroDigits = 9 - digits.length();
            }
            builder.append(digits);
            for (int i = 0; i < zeroDigits; i++) {
                builder.append('0');
            }

            builder.append(" UTC");
            return builder.toString();
        }
    }
}
