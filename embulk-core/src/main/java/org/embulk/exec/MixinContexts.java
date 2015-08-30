package org.embulk.exec;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.embulk.config.TaskReport;
import org.embulk.spi.MixinId;

public class MixinContexts
{
    private static final InheritableThreadLocal<MixinSession> session = new InheritableThreadLocal<MixinSession>();

    public static <T> T transaction(ReportCollector collector, Action<T> action)
    {
        MixinSession before = session.get();
        try {
            session.set(new MixinSession(collector));
            return action.run();
        }
        finally {
            session.set(before);
        }
    }

    public interface ReportCollector
    {
        List<Map<MixinId, TaskReport>> getMixinReports();
    }

    public static List<TaskReport> getTransactionReport(MixinId instanceId)
    {
        MixinSession s = session.get();
        if (s == null) {
            throw new NullPointerException("Not in Transaction");
        }
        return s.getTransactionReport(instanceId);
    }

    private static class MixinSession
    {
        private final ReportCollector collector;

        public MixinSession(ReportCollector collector)
        {
            this.collector = collector;
        }

        public List<TaskReport> getTransactionReport(MixinId instanceId)
        {
            List<Map<MixinId, TaskReport>> reports = collector.getMixinReports();

            ImmutableList.Builder<TaskReport> builder = ImmutableList.builder();
            for (Map<MixinId, TaskReport> report : reports) {
                TaskReport r = report.get(instanceId);
                if (r != null) {
                    builder.add(r);
                }
            }
            return builder.build();
        }
    }

    private static final InheritableThreadLocal<MixinTaskSession> taskSession = new InheritableThreadLocal<MixinTaskSession>();

    public static <T> ResultWithReports<T> runTask(Action<T> action)
    {
        MixinTaskSession before = taskSession.get();
        try {
            MixinTaskSession s = new MixinTaskSession(before);
            taskSession.set(s);
            T result = action.run();
            return new ResultWithReports<T>(result, s.getTaskReports());
        }
        finally {
            taskSession.set(before);
        }
    }

    public static void reportTask(MixinId instanceId, TaskReport taskReport)
    {
        MixinTaskSession ts = taskSession.get();
        if (ts == null) {
            throw new NullPointerException("Not running a task");
        }
        ts.reportTask(instanceId, taskReport);
    }

    public static MixinId newMixinId()
    {
        return new MixinId(UUID.randomUUID().toString());
    }

    private static class MixinTaskSession
    {
        private final MixinTaskSession nestedSession;
        private Map<MixinId, TaskReport> map = new HashMap<MixinId, TaskReport>();

        public MixinTaskSession(MixinTaskSession nestedSession)
        {
            this.nestedSession = nestedSession;
        }

        public void reportTask(MixinId instanceId, TaskReport report)
        {
            map.put(instanceId, report);
            if (nestedSession != null) {
                nestedSession.reportTask(instanceId, report);
            }
        }

        public Map<MixinId, TaskReport> getTaskReports()
        {
            return ImmutableMap.copyOf(map);
        }
    }

    public static interface Action <T>
    {
        T run();
    }

    public static class ResultWithReports <T>
    {
        private final T result;
        private final Map<MixinId, TaskReport> reports;

        public ResultWithReports(T result, Map<MixinId, TaskReport> reports)
        {
            this.result = result;
            this.reports = reports;
        }

        public T getResult()
        {
            return result;
        }

        public Map<MixinId, TaskReport> getReports()
        {
            return reports;
        }
    }
}
