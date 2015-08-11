package org.embulk;

import java.util.Arrays;
import java.util.List;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.annotations.Beta;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.embulk.config.ModelManager;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigLoader;
import org.embulk.EmbulkService;
import org.embulk.exec.BulkLoader;
import org.embulk.exec.GuessExecutor;
import org.embulk.exec.PreviewExecutor;
import org.embulk.exec.PreviewResult;
import org.embulk.exec.ExecutionResult;
import org.embulk.exec.PartialExecutionException;
import org.embulk.exec.ResumeState;
import org.embulk.spi.ExecSession;
import org.embulk.guice.Bootstrap;
import org.embulk.guice.CloseableInjector;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Preconditions.checkNotNull;

@Beta
public class EmbulkEmbed
        implements AutoCloseable
{
    public static ConfigLoader newSystemConfigLoader()
    {
        return new ConfigLoader(new ModelManager(null, new ObjectMapper()));
    }

    private final CloseableInjector injector;
    private final BulkLoader bulkLoader;
    private final GuessExecutor guessExecutor;
    private final PreviewExecutor previewExecutor;

    public EmbulkEmbed(ConfigSource systemConfig, Module... additionalModules)
    {
        this(systemConfig, Arrays.asList(additionalModules));
    }

    public EmbulkEmbed(ConfigSource systemConfig,
            final Iterable<? extends Module> additionalModules)
    {
        this(systemConfig,
                new Function<List<Module>, Iterable<Module>>()
                {
                    public Iterable<Module> apply(List<Module> source)
                    {
                        return Iterables.concat(source, additionalModules);
                    }
                });
    }

    public EmbulkEmbed(ConfigSource systemConfig,
            Function<? super List<Module>, ? extends Iterable<? extends Module>> overrideModules)
    {
        this.injector = new Bootstrap()
            .requireExplicitBindings(false)
            .addModules(EmbulkService.standardModuleList(systemConfig))
            .overrideModules(overrideModules)
            .initializeCloseable();
        injector.getInstance(org.slf4j.ILoggerFactory.class);
        this.bulkLoader = new BulkLoader(injector, systemConfig);
        this.guessExecutor = injector.getInstance(GuessExecutor.class);
        this.previewExecutor = injector.getInstance(PreviewExecutor.class);
    }

    @Override
    public void close()
    {
        try {
            injector.close();
        }
        catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public Injector getInjector()
    {
        return injector;
    }

    public ModelManager getModelManager()
    {
        return injector.getInstance(ModelManager.class);
    }

    public ConfigLoader newConfigLoader()
    {
        return injector.getInstance(ConfigLoader.class);
    }

    public ExecSession.Builder sessionBuilder(ConfigSource execConfig)
    {
        return ExecSession.builder(injector).fromExecConfig(execConfig);
    }

    public ConfigDiff guess(ExecSession exec, ConfigSource config)
    {
        return guessExecutor.guess(exec, config);
    }

    public PreviewResult preview(ExecSession exec, ConfigSource config)
    {
        return previewExecutor.preview(exec, config);
    }

    public ExecutionResult run(ExecSession exec, ConfigSource config)
    {
        try {
            return bulkLoader.run(exec, config);
        } catch (PartialExecutionException partial) {
            try {
                bulkLoader.cleanup(config, partial.getResumeState());
            } catch (Throwable ex) {
                partial.addSuppressed(ex);
            }
            throw partial;
        }
    }

    public ResumableResult runResumable(ExecSession exec, ConfigSource config)
    {
        ExecutionResult result;
        try {
            result = bulkLoader.run(exec, config);
        } catch (PartialExecutionException partial) {
            return new ResumableResult(partial);
        }
        return new ResumableResult(result);
    }

    public ResumeAction resumeAction(ConfigSource config, ResumeState resumeState)
    {
        return new ResumeAction(config, resumeState);
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
    }

    public class ResumeAction
    {
        private final ConfigSource config;
        private final ResumeState resumeState;

        public ResumeAction(ConfigSource config, ResumeState resumeState)
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
}
