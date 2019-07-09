package org.embulk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.config.ModelManager;
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
import org.embulk.plugin.BuiltinPluginSourceModule;
import org.embulk.plugin.PluginClassLoaderModule;
import org.embulk.plugin.maven.MavenPluginSourceModule;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.ExecSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbulkEmbed {
    private EmbulkEmbed(final ConfigSource systemConfig, final Injector injector) {
        this.injector = injector;
        this.bulkLoader = injector.getInstance(BulkLoader.class);
        this.guessExecutor = injector.getInstance(GuessExecutor.class);
        this.previewExecutor = new PreviewExecutor();
    }

    public static class Bootstrap {
        public Bootstrap() {
            // We will stop using JSON-based system config using ObjectMapper.
            this.systemConfigLoader = new ConfigLoader(new ModelManager(null, new ObjectMapper()));
            this.systemConfig = this.systemConfigLoader.newConfigSource();
            this.moduleOverrides = new ArrayList<>();
            this.started = false;
        }

        @Deprecated  // To be removed. Plugins should not call this by themselves.
        public ConfigLoader getSystemConfigLoader() {
            return this.systemConfigLoader;
        }

        public Bootstrap setSystemConfigFromJson(final String systemConfigJson) {
            return this.setSystemConfig(this.systemConfigLoader.fromJsonString(systemConfigJson));
        }

        public Bootstrap setSystemConfig(final ConfigSource systemConfigGiven) {
            this.systemConfig = systemConfigGiven.deepCopy();
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

            ArrayList<Module> userModules = new ArrayList<>(standardModuleList(systemConfig));
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
            return new EmbulkEmbed(this.systemConfig, injector);
        }

        @Deprecated
        public EmbulkEmbed initializeCloseable() {
            return this.initialize();
        }

        private final ConfigLoader systemConfigLoader;
        private final List<Function<? super List<Module>, ? extends Iterable<? extends Module>>> moduleOverrides;

        private ConfigSource systemConfig;
        private boolean started;
    }

    @Deprecated  // To be removed.
    public static ConfigLoader newSystemConfigLoader() {
        return new ConfigLoader(new ModelManager(null, new ObjectMapper()));
    }

    public Injector getInjector() {
        return injector;
    }

    public ModelManager getModelManager() {
        return injector.getInstance(ModelManager.class);
    }

    public BufferAllocator getBufferAllocator() {
        return injector.getInstance(BufferAllocator.class);
    }

    public ConfigLoader newConfigLoader() {
        return injector.getInstance(ConfigLoader.class);
    }

    public ConfigDiff guess(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSession exec = newExecSession(config);
        try {
            return this.guessExecutor.guess(exec, config);
        } finally {
            exec.cleanup();
        }
    }

    public PreviewResult preview(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSession exec = newExecSession(config);
        try {
            return this.previewExecutor.preview(exec, config);
        } finally {
            exec.cleanup();
        }
    }

    public ExecutionResult run(final ConfigSource config) {
        logger.info("Started Embulk v" + EmbulkVersion.VERSION);

        final ExecSession exec = newExecSession(config);
        try {
            return this.bulkLoader.run(exec, config);
        } catch (final PartialExecutionException partial) {
            try {
                this.bulkLoader.cleanup(config, partial.getResumeState());
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

        final ExecSession exec = newExecSession(config);
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

    private ExecSession newExecSession(final ConfigSource config) {
        final ConfigSource execConfig = config.deepCopy().getNestedOrGetEmpty("exec");
        return ExecSession.builder(injector).fromExecConfig(execConfig).build();
    }

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
            final ExecutionResult result;
            try {
                result = bulkLoader.resume(config, resumeState);
            } catch (PartialExecutionException partial) {
                return new ResumableResult(partial);
            }
            return new ResumableResult(result);
        }

        public void cleanup() {
            bulkLoader.cleanup(config, resumeState);
        }

        private final ConfigSource config;
        private final ResumeState resumeState;
    }

    public void destroy() {
        throw new UnsupportedOperationException(
                "EmbulkEmbed#destroy() is no longer supported as JSR-250 lifecycle annotations are unsupported. "
                + "See https://github.com/embulk/embulk/issues/1047 for the details.");
    }

    static List<Module> standardModuleList(final ConfigSource systemConfig) {
        final ArrayList<Module> built = new ArrayList<>();
        built.add(new SystemConfigModule(systemConfig));
        built.add(new ExecModule(systemConfig));
        built.add(new ExtensionServiceLoaderModule(systemConfig));
        built.add(new PluginClassLoaderModule());
        built.add(new BuiltinPluginSourceModule());
        built.add(new MavenPluginSourceModule(systemConfig));
        built.add(new JRubyScriptingModule(systemConfig));
        return Collections.unmodifiableList(built);
    }

    private static final Logger logger = LoggerFactory.getLogger(EmbulkEmbed.class);

    private final Injector injector;
    private final BulkLoader bulkLoader;
    private final GuessExecutor guessExecutor;
    private final PreviewExecutor previewExecutor;
}
