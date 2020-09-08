package org.embulk.spi.util;

import java.util.List;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.ExecSession;
import org.embulk.spi.ExecSessionInternal;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.ProcessTask;
import org.embulk.spi.Schema;

/**
 * Utility class for handling an executor plugin.
 *
 * <p>It is considered to be an internal class, not for plugins. To make it explicit, {@link ExecutorsInternal} replaces it.
 */
@Deprecated
public abstract class Executors {
    private Executors() {}

    public interface ProcessStateCallback extends ExecutorsInternal.ProcessStateCallback {
        @Override
        public void started();

        @Override
        public void inputCommitted(TaskReport report);

        @Override
        public void outputCommitted(TaskReport report);
    }

    public static void process(ExecSession exec,
            ProcessTask task, int taskIndex,
            ProcessStateCallback callback) {
        if (!(exec instanceof ExecSessionInternal)) {
            throw new IllegalArgumentException(new ClassCastException());
        }
        final ExecSessionInternal execInternal = (ExecSessionInternal) exec;

        ExecutorsInternal.process(execInternal, task, taskIndex, callback);
    }

    public static void process(ExecSessionInternal exec, int taskIndex,
            InputPlugin inputPlugin, Schema inputSchema, TaskSource inputTaskSource,
            List<FilterPlugin> filterPlugins, List<Schema> filterSchemas, List<TaskSource> filterTaskSources,
            OutputPlugin outputPlugin, Schema outputSchema, TaskSource outputTaskSource,
            ProcessStateCallback callback) {
        if (!(exec instanceof ExecSessionInternal)) {
            throw new IllegalArgumentException(new ClassCastException());
        }
        final ExecSessionInternal execInternal = (ExecSessionInternal) exec;

        ExecutorsInternal.process(
                execInternal, taskIndex,
                inputPlugin, inputSchema, inputTaskSource,
                filterPlugins, filterSchemas, filterTaskSources,
                outputPlugin, outputSchema, outputTaskSource,
                callback);
    }

    public static Schema getInputSchema(List<Schema> schemas) {
        return ExecutorsInternal.getInputSchema(schemas);
    }

    public static Schema getOutputSchema(List<Schema> schemas) {
        return ExecutorsInternal.getOutputSchema(schemas);
    }
}
