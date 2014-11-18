package org.quickload.exec;

import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayDeque;
import com.google.common.base.Throwables;
import com.google.inject.Injector;
import org.quickload.config.ConfigSource;
import org.quickload.channel.ChannelAsynchronousCloseException;
import org.quickload.spi.ProcTask;

public abstract class PluginExecutors
{
    public static RuntimeException propagePluginExceptions(Throwable ex)
    {
        LinkedList<Throwable> suppressed = findShallowestHighPriorityExceptions(ex);
        Throwable representative = suppressed.removeFirst();
        for (Throwable s : suppressed) {
            representative.addSuppressed(s);
        }
        return Throwables.propagate(representative);
    }

    private static LinkedList<Throwable> findShallowestHighPriorityExceptions(Throwable ex)
    {
        LinkedList<Throwable> suppressed = new LinkedList<Throwable>();

        Queue<Throwable> queue = new ArrayDeque<Throwable>();
        queue.add(ex);
        while (!queue.isEmpty()) {
            Throwable candidate = queue.remove();
            if (isHighPriorityException(candidate)) {
                suppressed.addFirst(candidate);
                return suppressed;
            }
            suppressed.add(candidate);
            for (Throwable s : candidate.getSuppressed()) {
                queue.add(s);
            }
        }
        return suppressed;
    }

    private static boolean isHighPriorityException(Throwable ex)
    {
        return !(ex instanceof ChannelAsynchronousCloseException);
    }

    public static ProcTask newProcTask(Injector injector, ConfigSource systemConfig)
    {
        ProcTask proc = new ProcTask(injector);
        proc.setUniqueTransactionName(systemConfig.getString("transactionName", "TODO"));  // TODO set default value using current time
        // TODO get default time zone from systemConfig
        // TODO get start time from systemConfig
        return proc;
    }
}
