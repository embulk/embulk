package org.embulk.spi.util;

import java.util.List;
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
import org.embulk.spi.AbortTransactionResource;
import org.embulk.spi.CloseResource;
import org.embulk.plugin.compat.PluginWrappers;

public abstract class Executors
{
    private Executors() { }

    public interface ProcessStateCallback
    {
        public void started();

        public void inputCommitted(TaskReport report);

        public void outputCommitted(TaskReport report);
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

    public static void process(ExecSession exec, int taskIndex,
            InputPlugin inputPlugin, Schema inputSchema, TaskSource inputTaskSource,
            List<FilterPlugin> filterPlugins, List<Schema> filterSchemas, List<TaskSource> filterTaskSources,
            OutputPlugin outputPlugin, Schema outputSchema, TaskSource outputTaskSource,
            ProcessStateCallback callback)
    {
        TransactionalPageOutput tran = PluginWrappers.transactionalPageOutput(
            outputPlugin.open(outputTaskSource, outputSchema, taskIndex));

        callback.started();
        // here needs to use try-with-resource to add exception happend at close() or abort()
        // to suppressed exception. otherwise exception happend at close() or abort() overwrites
        // essential exception.
        try (CloseResource closer = new CloseResource(tran)) {
            try (AbortTransactionResource aborter = new AbortTransactionResource(tran)) {
                PageOutput filtered = Filters.open(filterPlugins, filterTaskSources, filterSchemas, tran);
                closer.closeThis(filtered);

                TaskReport inputTaskReport = inputPlugin.run(inputTaskSource, inputSchema, taskIndex, filtered);

                if (inputTaskReport == null) {
                    inputTaskReport = exec.newTaskReport();
                }
                callback.inputCommitted(inputTaskReport);

                TaskReport outputTaskReport = tran.commit();
                aborter.dontAbort();
                if (outputTaskReport == null) {
                    outputTaskReport = exec.newTaskReport();
                }
                callback.outputCommitted(outputTaskReport);  // TODO check output.finish() is called. wrap or abstract
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
