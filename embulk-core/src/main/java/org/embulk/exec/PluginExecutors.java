package org.embulk.exec;

import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayDeque;
import com.google.common.base.Throwables;
import com.google.inject.Injector;
import org.embulk.config.ConfigSource;
import org.embulk.channel.ChannelAsynchronousCloseException;
import org.embulk.spi.ExecTask;

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

    public static ExecTask newExecTask(Injector injector, ConfigSource config)
    {
        return ExecTask.loadFromConfig(injector, config);
    }
}
