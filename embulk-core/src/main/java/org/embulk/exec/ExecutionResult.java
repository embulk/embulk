package org.embulk.exec;

import java.util.List;
import org.embulk.config.ConfigDiff;

public class ExecutionResult
{
    private final ConfigDiff configDiff;
    private final boolean skipped;
    private final List<Throwable> ignoredExceptions;

    public ExecutionResult(ConfigDiff configDiff, boolean skipped, List<Throwable> ignoredExceptions)
    {
        this.configDiff = configDiff;
        this.skipped = skipped;
        this.ignoredExceptions = ignoredExceptions;
    }

    public ConfigDiff getConfigDiff()
    {
        return configDiff;
    }

    public boolean isSkipped()
    {
        return skipped;
    }

    public List<Throwable> getIgnoredExceptions()
    {
        return ignoredExceptions;
    }
}
