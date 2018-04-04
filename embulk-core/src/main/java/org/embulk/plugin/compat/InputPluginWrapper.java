package org.embulk.plugin.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;

public class InputPluginWrapper implements InputPlugin {
    public static InputPlugin wrapIfNecessary(InputPlugin object) {
        Method runMethod = wrapRunMethod(object);
        if (runMethod != null) {
            return new InputPluginWrapper(object, runMethod);
        }
        return object;
    }

    // TODO: Remove the CommitReport case by v0.10 or earlier.
    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/933
    private static Method wrapRunMethod(InputPlugin object) {
        try {
            Method m = object.getClass().getMethod("run", TaskSource.class, Schema.class, int.class, PageOutput.class);
            if (m.getReturnType().equals(org.embulk.config.CommitReport.class)) {
                return m;
            } else {
                return null;
            }
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private final InputPlugin object;
    private final Method runMethod;

    private InputPluginWrapper(InputPlugin object, Method runMethod) {
        this.object = object;
        this.runMethod = runMethod;
    }

    @Override
    public ConfigDiff transaction(ConfigSource config, InputPlugin.Control control) {
        return object.transaction(config, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource, Schema schema, int taskCount, InputPlugin.Control control) {
        return object.resume(taskSource, schema, taskCount, control);
    }

    @Override
    public void cleanup(TaskSource taskSource, Schema schema, int taskCount, List<TaskReport> successTaskReports) {
        object.cleanup(taskSource, schema, taskCount, successTaskReports);
    }

    @Override
    public TaskReport run(TaskSource taskSource, Schema schema, int taskIndex, PageOutput output) {
        if (runMethod != null) {
            try {
                return (TaskReport) runMethod.invoke(object, taskSource, schema, taskIndex, output);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (InvocationTargetException ex) {
                if (ex.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) ex.getCause();
                }
                if (ex.getCause() instanceof Error) {
                    throw (Error) ex.getCause();
                }
                throw new RuntimeException(ex.getCause());
            }

        } else {
            return object.run(taskSource, schema, taskIndex, output);
        }
    }

    @Override
    public ConfigDiff guess(ConfigSource config) {
        return object.guess(config);
    }
}
