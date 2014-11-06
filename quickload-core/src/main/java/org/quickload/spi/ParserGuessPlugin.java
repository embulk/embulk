package org.quickload.spi;

import java.util.List;
import org.quickload.config.NextConfig;
import org.quickload.config.ConfigSource;
import org.quickload.buffer.Buffer;

public interface ParserGuessPlugin
{
    public NextConfig guess(ProcTask proc, ConfigSource config,
            List<Buffer> samples);
}
