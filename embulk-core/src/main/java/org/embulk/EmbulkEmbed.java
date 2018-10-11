package org.embulk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import com.google.inject.Module;
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
import org.embulk.guice.Bootstrap;
import org.embulk.guice.LifeCycleInjector;
import org.embulk.jruby.JRubyScriptingModule;
import org.embulk.plugin.BuiltinPluginSourceModule;
import org.embulk.plugin.PluginClassLoaderModule;
import org.embulk.plugin.maven.MavenPluginSourceModule;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.ExecSession;

public class EmbulkEmbed {
    private EmbulkEmbed(final ConfigSource systemConfig, final LifeCycleInjector injector) {
        this.injector = injector;
        this.logger = injector.getInstance(org.slf4j.ILoggerFactory.class).getLogger(EmbulkEmbed.class.getName());
        this.bulkLoader = injector.getInstance(BulkLoader.class);
        this.guessExecutor = injector.getInstance(GuessExecutor.class);
        this.previewExecutor = injector.getInstance(PreviewExecutor.class);
    }

    public static class Bootstrap {
        public Bootstrap() {
            this.systemConfigLoader = newSystemConfigLoader();
            this.systemConfig = systemConfigLoader.newConfigSource();
            this.moduleOverrides = new ArrayList<>();
        }

        public ConfigLoader getSystemConfigLoader() {
            return this.systemConfigLoader;
        }

        public Bootstrap setSystemConfig(final ConfigSource systemConfig) {
            this.systemConfig = systemConfig.deepCopy();
            return this;
        }

        public Bootstrap addModules(final Module... additionalModules) {
            return this.addModules(Arrays.asList(additionalModules));
        }

        public Bootstrap addModules(final Iterable<? extends Module> additionalModules) {
            final ArrayList<Module> copyMutable = new ArrayList<>();
            for (final Module module : additionalModules) {
                copyMutable.add(module);
            }
            final List<Module> copy = Collections.unmodifiableList(copyMutable);
            return overrideModules(new com.google.common.base.Function<List<Module>, Iterable<Module>>() {
                    public Iterable<Module> apply(List<Module> modules) {
                        return Iterables.concat(modules, copy);
                    }
                });
        }

        @Deprecated
        public Bootstrap overrideModules(
                final com.google.common.base.Function<? super List<Module>, ? extends Iterable<? extends Module>> function) {
            this.moduleOverrides.add(function::apply);
            return this;
        }

        public EmbulkEmbed initialize() {
            return this.build(true);
        }

        public EmbulkEmbed initializeCloseable() {
            return this.build(false);
        }

        private EmbulkEmbed build(final boolean destroyOnShutdownHook) {
            org.embulk.guice.Bootstrap bootstrap = new org.embulk.guice.Bootstrap()
                    .requireExplicitBindings(false)
                    .addModules(standardModuleList(systemConfig));

            for (final Function<? super List<Module>, ? extends Iterable<? extends Module>> override : moduleOverrides) {
                bootstrap = bootstrap.overrideModules(override);
            }

            final LifeCycleInjector injector;
            if (destroyOnShutdownHook) {
                injector = bootstrap.initialize();
            } else {
                injector = bootstrap.initializeCloseable();
            }

            return new EmbulkEmbed(this.systemConfig, injector);
        }

        private final ConfigLoader systemConfigLoader;
        private final List<Function<? super List<Module>, ? extends Iterable<? extends Module>>> moduleOverrides;

        private ConfigSource systemConfig;
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
            return successfulResult != null;
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
            } catch (final PartialExecutionException partial) {
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

    public static ConfigLoader newSystemConfigLoader() {
        return new ConfigLoader(new ModelManager(null, new ObjectMapper()));
    }

    public Injector getInjector() {
        return this.injector;
    }

    public ModelManager getModelManager() {
        return this.injector.getInstance(ModelManager.class);
    }

    public BufferAllocator getBufferAllocator() {
        return this.injector.getInstance(BufferAllocator.class);
    }

    public ConfigLoader newConfigLoader() {
        return this.injector.getInstance(ConfigLoader.class);
    }

    public ConfigDiff guess(final ConfigSource config) {
        this.logger.info("Started Embulk v" + EmbulkVersion.VERSION);
        final ExecSession exec = newExecSession(config);
        try {
            return this.guessExecutor.guess(exec, config);
        } finally {
            exec.cleanup();
        }
    }

    public PreviewResult preview(final ConfigSource config) {
        this.logger.info("Started Embulk v" + EmbulkVersion.VERSION);
        final ExecSession exec = newExecSession(config);
        try {
            return this.previewExecutor.preview(exec, config);
        } finally {
            exec.cleanup();
        }
    }

    public ExecutionResult run(final ConfigSource config) {
        this.logger.info("Started Embulk v" + EmbulkVersion.VERSION);
        final ExecSession exec = newExecSession(config);
        try {
            return bulkLoader.run(exec, config);
        } catch (final PartialExecutionException partial) {
            try {
                bulkLoader.cleanup(config, partial.getResumeState());
            } catch (Throwable ex) {
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
        this.logger.info("Started Embulk v" + EmbulkVersion.VERSION);
        final ExecSession exec = newExecSession(config);
        try {
            final ExecutionResult result;
            try {
                result = bulkLoader.run(exec, config);
            } catch (final PartialExecutionException partial) {
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

    public ResumeStateAction resumeState(final ConfigSource config, final ConfigSource resumeStateConfig) {
        this.logger.info("Started Embulk v" + EmbulkVersion.VERSION);
        final ResumeState resumeState = resumeStateConfig.loadConfig(ResumeState.class);
        return new ResumeStateAction(config, resumeState);
    }

    public void destroy() {
        try {
            this.injector.destroy();
        } catch (final Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    static List<Module> standardModuleList(final ConfigSource systemConfig) {
        final ArrayList<Module> moduleListBuilt = new ArrayList<>();
        moduleListBuilt.add(new SystemConfigModule(systemConfig));
        moduleListBuilt.add(new ExecModule());
        moduleListBuilt.add(new ExtensionServiceLoaderModule(systemConfig));
        moduleListBuilt.add(new PluginClassLoaderModule(systemConfig));
        moduleListBuilt.add(new BuiltinPluginSourceModule());
        moduleListBuilt.add(new MavenPluginSourceModule(systemConfig));
        moduleListBuilt.add(new JRubyScriptingModule(systemConfig));
        return Collections.unmodifiableList(moduleListBuilt);
    }

    private ExecSession newExecSession(final ConfigSource config) {
        final ConfigSource execConfig = config.deepCopy().getNestedOrGetEmpty("exec");
        return ExecSession.builder(this.injector).fromExecConfig(execConfig).build();
    }

    private final LifeCycleInjector injector;
    private final org.slf4j.Logger logger;
    private final BulkLoader bulkLoader;
    private final GuessExecutor guessExecutor;
    private final PreviewExecutor previewExecutor;
}
