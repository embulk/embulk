package org.embulk.spi;

//import org.slf4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;

public class Exec
{
    private static final InheritableThreadLocal<ExecSession> session = new InheritableThreadLocal<ExecSession>();

    private Exec() { }

    public static <T> T doWith(ExecSession session, ExecAction<T> action) throws Exception
    {
        Exec.session.set(session);
        try {
            return action.run();
        } finally {
            Exec.session.set(null);
        }
    }

    public static ExecSession session()
    {
        ExecSession session = Exec.session.get();
        if (session == null) {
            throw new NullPointerException("Exec is used outside of Exec.doWith");
        }
        return session;
    }

    // TODO
    //public Logger getLogger()
    //{
    //    return session().getLogger();
    //}

    //public NoticeLogger notice()
    //{
    //    return session().notice();
    //}

    public static BufferAllocator getBufferAllocator()
    {
        return session().getBufferAllocator();
    }

    public static <T> T newPlugin(Class<T> iface, JsonNode typeConfig)
    {
        return session().newPlugin(iface, typeConfig);
    }

    public static <T extends Task> T loadConfig(ConfigSource config, Class<T> taskType)
    {
        return session().loadConfig(config, taskType);
    }

    public static <T extends Task> T loadTask(TaskSource taskSource, Class<T> taskType)
    {
        return session().loadTask(taskSource, taskType);
    }

    public static TaskSource dumpTask(Task task)
    {
        return session().dumpTask(task);
    }

}
