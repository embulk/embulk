package org.embulk.spi;

//import org.slf4j.Logger;
import org.embulk.config.Task;
import org.embulk.config.CommitReport;
import org.embulk.config.NextConfig;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.plugin.PluginType;
import org.embulk.jruby.JRubyBridge;

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

    public static <T> T newPlugin(Class<T> iface, PluginType type)
    {
        return session().newPlugin(iface, type);
    }

    public static CommitReport newCommitReport()
    {
        return session().newCommitReport();
    }

    public static NextConfig newNextConfig()
    {
        return session().newNextConfig();
    }

    public static ConfigSource newConfigSource()
    {
        return session().newConfigSource();
    }

    public static TaskSource newTaskSource()
    {
        return session().newTaskSource();
    }

    public static JRubyBridge getJRubyBridge()
    {
        return session().getJRubyBridge();
    }
}
