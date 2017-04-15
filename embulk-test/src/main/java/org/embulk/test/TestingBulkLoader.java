package org.embulk.test;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import java.util.List;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.BulkLoader;
import org.embulk.exec.ExecutionResult;
import org.embulk.exec.ForSystemConfig;
import org.embulk.exec.PreviewResult;
import org.embulk.exec.ResumeState;
import org.embulk.spi.Exec;
import org.embulk.spi.ExecSession;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkState;
import static org.embulk.plugin.InjectedPluginSource.registerPluginTo;

class TestingBulkLoader
        extends BulkLoader
{
    static Function<List<Module>, List<Module>> override()
    {
        return new Function<List<Module>, List<Module>>() {
            @Override
            public List<Module> apply(List<Module> modules)
            {
                Module override = new Module() {
                    public void configure(Binder binder)
                    {
                        binder.bind(BulkLoader.class).to(TestingBulkLoader.class);
                        registerPluginTo(binder, InputPlugin.class, "preview_result", PreviewResultInputPlugin.class);
                    }
                };
                return ImmutableList.of(Modules.override(modules).with(ImmutableList.of(override)));
            }
        };
    }

    /**
     * This plugin is used for TestingEmbulk.ParserBuilder.preview().
     */
    static final class PreviewResultInputPlugin
            implements InputPlugin
    {
        private static PreviewResult previewResult;

        static void setPreviewResult(PreviewResult result)
        {
            previewResult = result;
        }

        @Override
        public ConfigDiff transaction(ConfigSource config, Control control)
        {
            checkState(previewResult != null, "PreviewResult object must be set");
            return resume(config.loadConfig(Task.class).dump(), previewResult.getSchema(), 1, control);
        }

        @Override
        public ConfigDiff resume(TaskSource taskSource, Schema schema, int taskCount, Control control)
        {
            control.run(taskSource, schema, taskCount);
            return Exec.newConfigDiff();
        }

        @Override
        public void cleanup(TaskSource taskSource, Schema schema, int taskCount, List<TaskReport> successTaskReports)
        {
        }

        @Override
        public TaskReport run(TaskSource taskSource, Schema schema, int taskIndex, PageOutput output)
        {
            for (Page page : previewResult.getPages()) {
                output.add(page);
            }
            return Exec.newTaskReport();
        }

        @Override
        public ConfigDiff guess(ConfigSource config)
        {
            return Exec.newConfigDiff();
        }
    }

    @Inject
    public TestingBulkLoader(Injector injector,
            @ForSystemConfig ConfigSource systemConfig)
    {
        super(injector, systemConfig);
    }

    @Override
    protected LoaderState newLoaderState(Logger logger, ProcessPluginSet plugins)
    {
        return new TestingLoaderState(logger, plugins);
    }

    protected static class TestingLoaderState
            extends LoaderState
    {
        public TestingLoaderState(Logger logger, ProcessPluginSet plugins)
        {
            super(logger, plugins);
        }

        @Override
        public ExecutionResult buildExecuteResultWithWarningException(Throwable ex)
        {
            ExecutionResult result = super.buildExecuteResultWithWarningException(ex);
            return new TestingExecutionResult(result, buildResumeState(Exec.session()), Exec.session());
        }
    }

    static class TestingExecutionResult
            extends ExecutionResult
            implements TestingEmbulk.RunResult
    {
        private final Schema inputSchema;
        private final Schema outputSchema;
        private final List<TaskReport> inputTaskReports;
        private final List<TaskReport> outputTaskReports;

        public TestingExecutionResult(ExecutionResult orig,
                ResumeState resumeState, ExecSession session)
        {
            super(orig.getConfigDiff(), orig.isSkipped(), orig.getIgnoredExceptions());
            this.inputSchema = resumeState.getInputSchema();
            this.outputSchema = resumeState.getOutputSchema();
            this.inputTaskReports = buildReports(resumeState.getInputTaskReports(), session);
            this.outputTaskReports = buildReports(resumeState.getOutputTaskReports(), session);
        }

        private static List<TaskReport> buildReports(List<Optional<TaskReport>> optionalReports, ExecSession session)
        {
            ImmutableList.Builder<TaskReport> reports = ImmutableList.builder();
            for (Optional<TaskReport> report : optionalReports) {
                reports.add(report.or(session.newTaskReport()));
            }
            return reports.build();
        }

        @Override
        public Schema getInputSchema()
        {
            return inputSchema;
        }

        @Override
        public Schema getOutputSchema()
        {
            return outputSchema;
        }

        @Override
        public List<TaskReport> getInputTaskReports()
        {
            return inputTaskReports;
        }

        @Override
        public List<TaskReport> getOutputTaskReports()
        {
            return outputTaskReports;
        }
    }
}
