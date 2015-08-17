package org.embulk.plugin.compat;

import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import com.google.common.base.Throwables;
import org.embulk.config.CommitReport;
import org.embulk.config.TaskReport;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.InputPlugin;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskSource;

public class InputPluginWrapper
        implements InputPlugin
{
    public static InputPlugin wrapIfNecessary(InputPlugin object)
    {
        Method runMethod = wrapRunMethod(object);
        if (runMethod != null) {
            return new InputPluginWrapper(object, runMethod);
        }
        return object;
    }

    private static Method wrapRunMethod(InputPlugin object)
    {
        try {
            Method m = object.getClass().getMethod("run", TaskSource.class, Schema.class, int.class, PageOutput.class);
            if (m.getReturnType().equals(CommitReport.class)) {
                return m;
            } else {
                return null;
            }
        }
        catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private final InputPlugin object;
    private final Method runMethod;

    private InputPluginWrapper(InputPlugin object,
            Method runMethod)
    {
        this.object = object;
        this.runMethod = runMethod;
    }

    @Override
    public ConfigDiff transaction(ConfigSource config,
            InputPlugin.Control control)
    {
        return object.transaction(config, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            InputPlugin.Control control)
    {
        return object.resume(taskSource, schema, taskCount, control);
    }

    @Override
    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<TaskReport> successTaskReports)
    {
        object.cleanup(taskSource, schema, taskCount, successTaskReports);
    }

    @Override
    public TaskReport run(TaskSource taskSource,
            Schema schema, int taskIndex,
            PageOutput output)
    {
        if (runMethod != null) {
            try {
                return (TaskReport) runMethod.invoke(object, taskSource, schema, taskIndex, output);
            }
            catch (IllegalAccessException | IllegalArgumentException ex) {
                throw Throwables.propagate(ex);
            }
            catch (InvocationTargetException ex) {
                throw Throwables.propagate(ex.getCause());
            }

        } else {
            return object.run(taskSource, schema, taskIndex, output);
        }
    }

    @Override
    public ConfigDiff guess(ConfigSource config)
    {
        return object.guess(config);
    }
}
