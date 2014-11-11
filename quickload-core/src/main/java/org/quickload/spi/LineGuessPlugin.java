package org.quickload.spi;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.quickload.config.NextConfig;
import org.quickload.config.ConfigSource;
import org.quickload.buffer.Buffer;

public abstract class LineGuessPlugin
        implements GuessPlugin
{
    @Override
    public NextConfig guess(ProcTask proc, ConfigSource config,
            Buffer sample)
    {
        LineDecoderTask task;
        try {
            task = proc.loadConfig(config, LineDecoderTask.class);
        } catch (Exception ex) {
            return new NextConfig();
        }

        LineDecoder decoder = new LineDecoder(ImmutableList.of(sample), task);
        List<String> lines = ImmutableList.copyOf(decoder);
        return guessLines(proc, config, lines);
    }

    public abstract NextConfig guessLines(ProcTask proc, ConfigSource config,
            List<String> lines);
}
