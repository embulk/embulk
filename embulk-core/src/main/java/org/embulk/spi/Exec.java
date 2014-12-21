package org.embulk.spi;

//import org.slf4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;

public class Exec
{
    private static final InheritableThreadLocal<ExecSession> session = new InheritableThreadLocal<ExecSession>();

    private Exec() { }

    public <T> T doWith(ExecSession session, ExecAction<T> action) throws Exception
    {
        this.session.set(session);
        try {
            return action.run();
        } finally {
            this.session.set(null);
        }
    }

    public ExecSession session()
    {
        ExecSession session = session.get();
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

    public BufferAllocator getBufferAllocator()
    {
        return session().getBufferAllocator();
    }

    public <T> T newPlugin(Class<T> iface, JsonNode typeConfig)
    {
        return session().newPlugin(iface, typeConfig);
    }
}
