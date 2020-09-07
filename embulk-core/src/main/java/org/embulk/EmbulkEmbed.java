package org.embulk;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.exec.BulkLoader;
import org.embulk.exec.ExecModule;
import org.embulk.exec.ExecutionResult;
import org.embulk.exec.ExtensionServiceLoaderModule;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.PartialExecutionException;
import org.embulk.exec.PreviewExecutor;
import org.embulk.exec.PreviewResult;
import org.embulk.exec.ResumeState;
import org.embulk.exec.SystemConfigModule;
import org.embulk.exec.TransactionStage;
import org.embulk.jruby.JRubyScriptingModule;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.ExecSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbulkEmbed {
    private EmbulkEmbed(final Injector injector, final EmbulkSystemProperties embulkSystemProperties) {
        this.injector = injector;
        this.embulkSystemProperties = embulkSystemProperties;

        this.bulkLoader = injector.getInstance(BulkLoader.class);
        this.guessExecutor = injector.getInstance(GuessExecutor.class);
        this.previewExecutor = new PreviewExecutor();
    }

    public static class Bootstrap {
        public Bootstrap() {
            this.embulkSystemPropertiesBuilt = new Properties();
            this.moduleOverrides = new ArrayList<>();
            this.started = false;
        }

        public Bootstrap setEmbulkSystemProperties(final Properties propertiesGiven) {
            this.embulkSystemPropertiesBuilt = propertiesGiven;
            return this;
        }

        public Bootstrap addModules(final Module... additionalModules) {
            return this.addModules(Arrays.asList(additionalModules));
        }

        public Bootstrap addModules(final Iterable<? extends Module> additionalModules) {
            return this.overrideModulesJavaUtil(
                modules -> {
                    final ArrayList<Module> concatenated = new ArrayList<>(modules);
                    for (final Module module : additionalModules) {
                        concatenated.add(module);
                    }
                    return Collections.unmodifiableList(concatenated);
                });
        }

        /**
         * Add a module overrider function with Google Guava's Function.
         *
         * <p>Caution: this method is not working as intended. It is kept just for binary compatibility.
         *
         * @param function  the module overrider function
         * @return this
         */
        @Deprecated
        public Bootstrap overrideModules(
                final com.google.common.base.Function<? super List<Module>, ? extends Iterable<? extends Module>> function) {
            // TODO: Enable this logging.
            // logger.warn("EmbulkEmbed.Bootstrap#overrideModules is deprecated.",
            //             new Throwable("Logging the stack trace to help identifying the caller."));
            this.moduleOverrides.add(function::apply);
            return this;
        }

        /**
         * Add a module overrider function with java.util.function.Function.
         *
         * <p>This method is not disclosed intentionally. We doubt we need to provide the overriding feature to users.
         *
         * @param function  the module overrider function
         * @return this
         */
        private Bootstrap overrideModulesJavaUtil(
                final Function<? super List<Module>, ? extends Iterable<? extends Module>> function) {
            this.moduleOverrides.add(function);
            return this;
        }

        public EmbulkEmbed initialize() {
            if (this.started) {
                throw new IllegalStateException("System already initialized");
            }
            this.started = true;

            final ArrayList<Module> modulesListBuilt = new ArrayList<>();

            final EmbulkSystemProperties embulkSystemProperties = EmbulkSystemProperties.of(this.embulkSystemPropertiesBuilt);
            ArrayList<Module> userModules = new ArrayList<>(standardModuleList(embulkSystemProperties));
            for (final Function<? super List<Module>, ? extends Iterable<? extends Module>> override : this.moduleOverrides) {
                final Iterable<? extends Module> overridden = override.apply(userModules);
                userModules = new ArrayList<Module>();
                for (final Module module : overridden) {
                    userModules.add(module);
                }
            }
            modulesListBuilt.addAll(userModules);

            modulesListBuilt.add(new Module() {
                    @Override
                    public void configure(final Binder binder) {
                        binder.disableCircularProxies();
                    }
                });

            final Injector injector = Guice.createInjector(Stage.PRODUCTION, Collections.unmodifiableList(modulesListBuilt));
            return new EmbulkEmbed(injector, embulkSystemProperties);
        }

        @Deprecated
        public EmbulkEmbed initializeCloseable() {
            return this.initialize();
        }

        private final List<Function<? super List<Module>, ? extends Iterable<? extends Module>>> moduleOverrides;

        // We are trying to represent the "system config" in java.util.Properties, instead of ConfigSource.
        // TODO: Make this java.util.Properties use as system config. See: https://github.com/embulk/embulk/issues/1159
        private Properties embulkSystemPropertiesBuilt;

        private boolean started;
    }

    public Injector getInjector() {
        return injector;
    }

    @Deprecated  // https://github.com/embulk/embulk/issues/1304
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1304
    public org.embulk.config.ModelManager getModelManager() {
        return injector.getInstance(org.embulk.config.ModelManager.class);
    }

    public BufferAllocator getBufferAllocator() {
        return injector.getInstance(BufferAllocator.class);
    }

    public ConfigLoader newConfigLoader() {
        return injector.getInstance(ConfigLoader.class);
    }

    public ConfigDiff guess(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSession exec = this.newExecSession(config.deepCopy().getNestedOrGetEmpty("exec"));
        try {
            return this.guessExecutor.guess(exec, config);
        } finally {
            exec.cleanup();
        }
    }

    public PreviewResult preview(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSession exec = this.newExecSession(config.deepCopy().getNestedOrGetEmpty("exec"));
        try {
            return this.previewExecutor.preview(exec, config);
        } finally {
            exec.cleanup();
        }
    }

    public ExecutionResult run(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSession exec = this.newExecSession(config.deepCopy().getNestedOrGetEmpty("exec"));
        try {
            return this.bulkLoader.run(exec, config);
        } catch (final PartialExecutionException partial) {
            final ExecSession cleanupExec = this.newExecSession(partial.getResumeState().getExecSessionConfigSource());
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

        final ExecSession exec = this.newExecSession(config.deepCopy().getNestedOrGetEmpty("exec"));
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

    private ExecSession newExecSession(final ConfigSource execConfig) {
        return ExecSession.builder(this.injector)
                .setEmbulkSystemProperties(this.embulkSystemProperties)
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
            final ExecSession exec = newExecSession(this.resumeState.getExecSessionConfigSource());
            final ExecutionResult result;
            try {
                result = bulkLoader.resume(exec, config, resumeState);
            } catch (PartialExecutionException partial) {
                return new ResumableResult(partial);
            }
            return new ResumableResult(result);
        }

        public void cleanup() {
            final ExecSession exec = newExecSession(this.resumeState.getExecSessionConfigSource());
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

    static List<Module> standardModuleList(final EmbulkSystemProperties embulkSystemProperties) {
        final ArrayList<Module> built = new ArrayList<>();
        built.add(new SystemConfigModule(embulkSystemProperties));
        built.add(new ExecModule(embulkSystemProperties));
        built.add(new ExtensionServiceLoaderModule(embulkSystemProperties));
        built.add(new JRubyScriptingModule());
        return Collections.unmodifiableList(built);
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

    // TODO: Remove them finally.
    static final Set<String> PARENT_FIRST_PACKAGES = readPropertyKeys("/embulk/parent_first_packages.properties");
    static final Set<String> PARENT_FIRST_RESOURCES = readPropertyKeys("/embulk/parent_first_resources.properties");

    private static final Logger logger = LoggerFactory.getLogger(EmbulkEmbed.class);

    private final Injector injector;
    private final EmbulkSystemProperties embulkSystemProperties;

    private final BulkLoader bulkLoader;
    private final GuessExecutor guessExecutor;
    private final PreviewExecutor previewExecutor;
}
