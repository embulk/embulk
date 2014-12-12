package org.embulk.spi;

import org.embulk.config.NextConfig;
import org.embulk.config.ConfigSource;
import org.embulk.buffer.Buffer;

public interface GuessPlugin
{
    public NextConfig guess(ExecTask exec, ConfigSource config,
            Buffer sample);
}
