package org.embulk.spi.util;

import java.util.List;
import java.util.Map;
import org.embulk.config.TaskSource;
import org.embulk.config.TaskReport;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ProcessState;
import org.embulk.spi.Schema;
import org.embulk.spi.TransactionalPageOutput;
import org.embulk.spi.PageOutput;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ProcessTask;
import org.embulk.spi.MixinId;
import org.embulk.exec.MixinContexts;
import org.embulk.plugin.compat.PluginWrappers;

public abstract class Executors
{
    private Executors() { }

    public interface ProcessStateCallback
    {
        public void started();

        public void inputCommitted(TaskReport report);

        public void outputCommitted(TaskReport report, Map<MixinId, TaskReport> mixinReports);
    }

    public static void process(ExecSession exec,
            ProcessTask task, int taskIndex,
            ProcessStateCallback callback)
    {
        InputPlugin inputPlugin = exec.newPlugin(InputPlugin.class, task.getInputPluginType());
        List<FilterPlugin> filterPlugins = Filters.newFilterPlugins(exec, task.getFilterPluginTypes());
        OutputPlugin outputPlugin = exec.newPlugin(OutputPlugin.class, task.getOutputPluginType());

        // TODO assert task.getExecutorSchema().equals task.getOutputSchema()

        process(exec, taskIndex,
                inputPlugin, task.getInputSchema(), task.getInputTaskSource(),
                filterPlugins, task.getFilterSchemas(), task.getFilterTaskSources(),
                outputPlugin, task.getOutputSchema(), task.getOutputTaskSource(),
                callback);
    }

    public static void process(final ExecSession exec, final int taskIndex,
            final InputPlugin inputPlugin, final Schema inputSchema, final TaskSource inputTaskSource,
            List<FilterPlugin> filterPlugins, List<Schema> filterSchemas, List<TaskSource> filterTaskSources,
            OutputPlugin outputPlugin, Schema outputSchema, TaskSource outputTaskSource,
            final ProcessStateCallback callback)
    {
        TransactionalPageOutput tran = PluginWrappers.transactionalPageOutput(
            outputPlugin.open(outputTaskSource, outputSchema, taskIndex));

        PageOutput closeThis = tran;
        callback.started();
        try {
            final PageOutput filtered = closeThis = Filters.open(filterPlugins, filterTaskSources, filterSchemas, tran);

            final TransactionalPageOutput out = tran;
            MixinContexts.ResultWithReports<TaskReport> r =
                MixinContexts.runTask(new MixinContexts.Action<TaskReport>() {
                    public TaskReport run()
                    {
                        TaskReport inputTaskReport = inputPlugin.run(inputTaskSource, inputSchema, taskIndex, filtered);

                        if (inputTaskReport == null) {
                            inputTaskReport = exec.newTaskReport();
                        }
                        callback.inputCommitted(inputTaskReport);

                        return out.commit();
                    }
                });
            TaskReport outputTaskReport = r.getResult();
            tran = null;
            if (outputTaskReport == null) {
                outputTaskReport = exec.newTaskReport();
            }
            // TODO check output.finish() is called. wrap or abstract
            callback.outputCommitted(outputTaskReport, r.getReports());

        } finally {
            try {
                if (tran != null) {
                    tran.abort();
                }
            } finally {
                closeThis.close();
            }
        }
    }

    public static Schema getInputSchema(List<Schema> schemas)
    {
        return schemas.get(0);
    }

    public static Schema getOutputSchema(List<Schema> schemas)
    {
        return schemas.get(schemas.size() - 1);
    }
}
