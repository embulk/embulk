package org.embulk.plugin.compat;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import com.google.common.base.Throwables;
import org.embulk.config.TaskReport;
import org.embulk.config.CommitReport;
import org.embulk.spi.Buffer;
import org.embulk.spi.TransactionalFileInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionalFileInputWrapper
        implements TransactionalFileInput
{
    private final Logger logger = LoggerFactory.getLogger(PluginWrappers.class);

    public static TransactionalFileInput wrapIfNecessary(TransactionalFileInput object)
    {
        Method runMethod = wrapCommitMethod(object);
        if (runMethod != null) {
            return new TransactionalFileInputWrapper(object, runMethod);
        }
        return object;
    }

    private static Method wrapCommitMethod(TransactionalFileInput object)
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

    private final TransactionalFileInput object;
    private final Method commitMethod;

    private TransactionalFileInputWrapper(TransactionalFileInput object,
            Method commitMethod)
    {
        this.object = object;
        this.commitMethod = commitMethod;
        logger.warn("A file input plugin is compiled with old Embulk plugin API. Please update the plugin version using \"embulk gem install\" command, or contact a developer of the plugin to upgrade the plugin code using \"embulk migrate\" command: {}", object.getClass());
    }

    @Override
    public Buffer poll()
    {
        return object.poll();
    }

    @Override
    public boolean nextFile()
    {
        return object.nextFile();
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

