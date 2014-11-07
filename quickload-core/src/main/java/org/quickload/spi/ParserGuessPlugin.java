package org.quickload.spi;

import org.quickload.config.NextConfig;
import org.quickload.config.ConfigSource;
import org.quickload.buffer.Buffer;

public interface ParserGuessPlugin
{
    public NextConfig guess(ProcTask proc, ConfigSource config,
            Buffer sample);
}
