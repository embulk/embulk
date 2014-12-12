package org.embulk.spi;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.embulk.config.NextConfig;
import org.embulk.config.ConfigSource;
import org.embulk.buffer.Buffer;

public abstract class LineGuessPlugin
        implements GuessPlugin
{
    @Override
    public NextConfig guess(ExecTask exec, ConfigSource config,
            Buffer sample)
    {
        LineDecoderTask task;
        try {
            task = exec.loadConfig(config, LineDecoderTask.class);
        } catch (Exception ex) {
            return new NextConfig();
        }

        LineDecoder decoder = new LineDecoder(ImmutableList.of(sample), task);
        List<String> lines = ImmutableList.copyOf(decoder);
        return guessLines(exec, config, lines);
    }

    public abstract NextConfig guessLines(ExecTask exec, ConfigSource config,
            List<String> lines);
}
