package org.quickload.exec;

import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayDeque;
import com.google.common.base.Throwables;
import org.quickload.channel.ChannelAsynchronousCloseException;

public abstract class PluginExecutors
{
    public static RuntimeException propagePluginExceptions(Throwable ex)
    {
        LinkedList<Throwable> suppressed = findShallowestHighPriorityExceptions(ex);
        Throwable representative = suppressed.removeFirst();
        System.out.println("list: "+suppressed);
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
            System.out.println("candidate: "+candidate);
            if (isHighPriorityException(candidate)) {
                suppressed.addFirst(candidate);
                return suppressed;
            }
            System.out.println("suppressed: "+candidate);
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
}
