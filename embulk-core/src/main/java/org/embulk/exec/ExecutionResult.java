package org.embulk.exec;

import java.util.List;
import org.embulk.config.NextConfig;

public class ExecutionResult
{
    private final NextConfig nextConfig;
    private final List<Throwable> ignoredExceptions;

    public ExecutionResult(NextConfig nextConfig, List<Throwable> ignoredExceptions)
    {
        this.nextConfig = nextConfig;
        this.ignoredExceptions = ignoredExceptions;
    }

    public NextConfig getNextConfig()
    {
        return nextConfig;
    }

    public List<Throwable> getIgnoredExceptions()
    {
        return ignoredExceptions;
    }
}
