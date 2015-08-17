package org.embulk.plugin.compat;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import com.google.common.base.Throwables;
import org.embulk.config.TaskReport;
import org.embulk.config.CommitReport;
import org.embulk.spi.Page;
import org.embulk.spi.TransactionalPageOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionalPageOutputWrapper
        implements TransactionalPageOutput
{
    private final Logger logger = LoggerFactory.getLogger(PluginWrappers.class);

    public static TransactionalPageOutput wrapIfNecessary(TransactionalPageOutput object)
    {
        Method runMethod = wrapCommitMethod(object);
        if (runMethod != null) {
            return new TransactionalPageOutputWrapper(object, runMethod);
        }
        return object;
    }

    private static Method wrapCommitMethod(TransactionalPageOutput object)
    {
        try {
            Method m = object.getClass().getMethod("commit");
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

    private final TransactionalPageOutput object;
    private final Method commitMethod;

    private TransactionalPageOutputWrapper(TransactionalPageOutput object,
            Method commitMethod)
    {
        this.object = object;
        this.commitMethod = commitMethod;
        logger.warn("An output plugin is compiled with old Embulk plugin API. Please update the plugin version using \"embulk gem install\" command, or contact a developer of the plugin to upgrade the plugin code using \"embulk migrate\" command: {}", object.getClass());
    }

    @Override
    public void add(Page page)
    {
        object.add(page);
    }

    @Override
    public void finish()
    {
        object.finish();
    }

    @Override
    public void close()
    {
        object.close();
    }

    @Override
    public void abort()
    {
        object.abort();
    }

    @Override
    public TaskReport commit()
    {
        if (commitMethod != null) {
            try {
                return (TaskReport) commitMethod.invoke(object);
            }
            catch (IllegalAccessException | IllegalArgumentException ex) {
                throw Throwables.propagate(ex);
            }
            catch (InvocationTargetException ex) {
                throw Throwables.propagate(ex.getCause());
            }

        } else {
            return object.commit();
        }
    }
}
