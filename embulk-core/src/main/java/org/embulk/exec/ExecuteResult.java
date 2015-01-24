package org.embulk.exec;

import java.util.List;
import org.embulk.config.NextConfig;

public class ExecuteResult
{
    private final NextConfig nextConfig;

    public ExecuteResult(NextConfig nextConfig)
    {
        this.nextConfig = nextConfig;
    }

    public NextConfig getNextConfig()
    {
        return nextConfig;
    }
}
