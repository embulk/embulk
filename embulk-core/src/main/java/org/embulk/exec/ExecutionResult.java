package org.embulk.exec;

import java.util.List;
import org.embulk.config.ConfigDiff;

public class ExecutionResult
{
    private final ConfigDiff configDiff;
    private final List<Throwable> ignoredExceptions;

    public ExecutionResult(ConfigDiff configDiff, List<Throwable> ignoredExceptions)
    {
        this.configDiff = configDiff;
        this.ignoredExceptions = ignoredExceptions;
    }

    public ConfigDiff getConfigDiff()
    {
        return configDiff;
    }

    public List<Throwable> getIgnoredExceptions()
    {
        return ignoredExceptions;
    }
}
