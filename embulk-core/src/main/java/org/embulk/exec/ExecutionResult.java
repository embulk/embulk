package org.embulk.exec;

import java.util.List;
import org.embulk.config.ConfigDiff;

public class ExecutionResult {
    private final ConfigDiff configDiff;
    private final boolean skipped;
    private final List<Throwable> ignoredExceptions;
    private StatisticsResult statisticsResult;

    public ExecutionResult(ConfigDiff configDiff, boolean skipped, List<Throwable> ignoredExceptions) {
        this.configDiff = configDiff;
        this.skipped = skipped;
        this.ignoredExceptions = ignoredExceptions;
    }

    public ExecutionResult(final ConfigDiff configDiff, final boolean skipped, final List<Throwable> ignoredExceptions, final StatisticsResult statisticsResult)
    {
        this(configDiff, skipped, ignoredExceptions);
        this.statisticsResult = statisticsResult;
    }

    public ConfigDiff getConfigDiff() {
        return configDiff;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public List<Throwable> getIgnoredExceptions() {
        return ignoredExceptions;
    }

    public StatisticsResult getStatisticsResult()
    {
        return statisticsResult;
    }
}
