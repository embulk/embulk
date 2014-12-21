package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;

public abstract class Decoders
{
    private Decoders() { }

    public List<DecoderPlugin> newDecoderPlugins(ExecSession exec, List<ConfigSource> configs)
    {
        // TODO
        return null;
    }

    public interface Control
    {
        public void run(List<TaskSource> taskSources);
    }

    public static void transaction(List<DecoderPlugin> decoderPlugins, List<ConfigSource> configs,
            Decoders.Control control)
    {
        // TODO
    }

    public static FileInput open(List<DecoderPlugin> decoderPlugins, List<TaskSource> taskSources,
            FileInput input)
    {
        // TODO
    }
}
