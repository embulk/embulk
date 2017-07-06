package org.embulk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.embulk.config.ModelManager;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigLoader;
import org.embulk.exec.BulkLoader;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.PreviewExecutor;
import org.embulk.exec.PreviewResult;
import org.embulk.exec.ExecutionResult;
import org.embulk.exec.PartialExecutionException;
import org.embulk.exec.ResumeState;
import org.embulk.exec.TransactionStage;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.ExecSession;
import org.embulk.guice.Bootstrap;
import org.embulk.guice.LifeCycleInjector;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Preconditions.checkNotNull;

public class EmbulkEmbed
{
    public static ConfigLoader newSystemConfigLoader()
    {
        return new ConfigLoader(new ModelManager(null, new ObjectMapper()));
    }

    public static class Bootstrap
    {
        private final ConfigLoader systemConfigLoader;

        private ConfigSource systemConfig;

        private final List<Function<? super List<Module>, ? extends Iterable<? extends Module>>> moduleOverrides;

        public Bootstrap()
        {
            this.systemConfigLoader = newSystemConfigLoader();
            this.systemConfig = systemConfigLoader.newConfigSource();
            this.moduleOverrides = new ArrayList<>();
        }

        public ConfigLoader getSystemConfigLoader()
        {
            return systemConfigLoader;
        }

        public Bootstrap setSystemConfig(ConfigSource systemConfig)
        {
            this.systemConfig = systemConfig.deepCopy();
            return this;
        }

        public Bootstrap addModules(Module... additionalModules)
        {
            return addModules(Arrays.asList(additionalModules));
        }

        public Bootstrap addModules(Iterable<? extends Module> additionalModules)
        {
            final List<Module> copy = ImmutableList.copyOf(additionalModules);
            return overrideModules(
                    new Function<List<Module>, Iterable<Module>>()
                    {
                        public Iterable<Module> apply(List<Module> modules)
                        {
                            return Iterables.concat(modules, copy);
                        }
                    });
        }

        public Bootstrap overrideModules(Function<? super List<Module>, ? extends Iterable<? extends Module>> function)
        {
            moduleOverrides.add(function);
            return this;
        }

        public EmbulkEmbed initialize()
        {
            return build(true);
        }

        public EmbulkEmbed initializeCloseable()
        {
            return build(false);
        }

        private EmbulkEmbed build(boolean destroyOnShutdownHook)
        {
            org.embulk.guice.Bootstrap bootstrap = new org.embulk.guice.Bootstrap()
                .requireExplicitBindings(false)
                .addModules(EmbulkService.standardModuleList(systemConfig));

            for (Function<? super List<Module>, ? extends Iterable<? extends Module>> override : moduleOverrides) {
                bootstrap = bootstrap.overrideModules(override);
            }

            LifeCycleInjector injector;
            if (destroyOnShutdownHook) {
                injector = bootstrap.initialize();
            } else {
                injector = bootstrap.initializeCloseable();
            }

            return new EmbulkEmbed(systemConfig, injector);
        }
    }

    private final LifeCycleInjector injector;
    private final BulkLoader bulkLoader;
    private final GuessExecutor guessExecutor;
    private final PreviewExecutor previewExecutor;

    EmbulkEmbed(ConfigSource systemConfig, LifeCycleInjector injector)
    {
        this.injector = injector;
        injector.getInstance(org.slf4j.ILoggerFactory.class);
        this.bulkLoader = injector.getInstance(BulkLoader.class);
        this.guessExecutor = injector.getInstance(GuessExecutor.class);
        this.previewExecutor = injector.getInstance(PreviewExecutor.class);
    }

    public Injector getInjector()
    {
        return injector;
    }

    public ModelManager getModelManager()
    {
        return injector.getInstance(ModelManager.class);
    }

    public BufferAllocator getBufferAllocator()
    {
        return injector.getInstance(BufferAllocator.class);
    }

    public ConfigLoader newConfigLoader()
    {
        return injector.getInstance(ConfigLoader.class);
    }

    public ConfigDiff guess(ConfigSource config)
    {
        ExecSession exec = newExecSession(config);
        try {
            return guessExecutor.guess(exec, config);
        }
        finally {
            exec.cleanup();
        }
    }

    public PreviewResult preview(ConfigSource config)
    {
        ExecSession exec = newExecSession(config);
        try {
            return previewExecutor.preview(exec, config);
        }
        finally {
            exec.cleanup();
        }
    }

    public ExecutionResult run(ConfigSource config)
    {
        ExecSession exec = newExecSession(config);
        try {
            return bulkLoader.run(exec, config);
        }
        catch (PartialExecutionException partial) {
            try {
                bulkLoader.cleanup(config, partial.getResumeState());
            } catch (Throwable ex) {
                partial.addSuppressed(ex);
            }
            throw partial;
        }
        finally {
            try {
                exec.cleanup();
            }
            catch (Exception ex) {
                // TODO add this exception to ExecutionResult.getIgnoredExceptions
                // or partial.addSuppressed
                ex.printStackTrace(System.err);
            }
        }
    }

    public ResumableResult runResumable(ConfigSource config)
    {
        ExecSession exec = newExecSession(config);
        try {
            ExecutionResult result;
            try {
                result = bulkLoader.run(exec, config);
            } catch (PartialExecutionException partial) {
                return new ResumableResult(partial);
            }
            return new ResumableResult(result);
        }
        finally {
            try {
                exec.cleanup();
            }
            catch (Exception ex) {
                // TODO add this exception to ExecutionResult.getIgnoredExceptions
                // or partial.addSuppressed
                ex.printStackTrace(System.err);
            }
        }
    }

    private ExecSession newExecSession(ConfigSource config)
    {
        ConfigSource execConfig = config.deepCopy().getNestedOrGetEmpty("exec");
        return ExecSession.builder(injector).fromExecConfig(execConfig).build();
    }

    public ResumeStateAction resumeState(ConfigSource config, ConfigSource resumeStateConfig)
    {
        ResumeState resumeState = resumeStateConfig.loadConfig(ResumeState.class);
        return new ResumeStateAction(config, resumeState);
    }

    public static class ResumableResult
    {
        private final ExecutionResult successfulResult;
        private final PartialExecutionException partialExecutionException;

        public ResumableResult(PartialExecutionException partialExecutionException)
        {
            this.successfulResult = null;
            this.partialExecutionException = checkNotNull(partialExecutionException);
        }

        public ResumableResult(ExecutionResult successfulResult)
        {
            this.successfulResult = checkNotNull(successfulResult);
            this.partialExecutionException = null;
        }

        public boolean isSuccessful()
        {
            return successfulResult != null;
        }

        public ExecutionResult getSuccessfulResult()
        {
            checkState(successfulResult != null);
            return successfulResult;
        }

        public Throwable getCause()
        {
            checkState(partialExecutionException != null);
            return partialExecutionException.getCause();
        }

        public ResumeState getResumeState()
        {
            checkState(partialExecutionException != null);
            return partialExecutionException.getResumeState();
        }

        public TransactionStage getTransactionStage()
        {
            return partialExecutionException.getTransactionStage();
        }
    }

    public class ResumeStateAction
    {
        private final ConfigSource config;
        private final ResumeState resumeState;

        public ResumeStateAction(ConfigSource config, ResumeState resumeState)
        {
            this.config = config;
            this.resumeState = resumeState;
        }

        public ResumableResult resume()
        {
            ExecutionResult result;
            try {
                result = bulkLoader.resume(config, resumeState);
            } catch (PartialExecutionException partial) {
                return new ResumableResult(partial);
            }
            return new ResumableResult(result);
        }

        public void cleanup()
        {
            bulkLoader.cleanup(config, resumeState);
        }
    }

    public void destroy()
    {
        try {
            injector.destroy();
        }
        catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }
}
