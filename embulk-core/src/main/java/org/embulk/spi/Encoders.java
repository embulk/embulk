package org.embulk.spi;

import java.util.List;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;

public abstract class Encoders
{
    private Encoders() { }

    public List<EncoderPlugin> newEncoderPlugins(ExecSession exec, List<ConfigSource> configs)
    {
        // TODO
        return null;
    }

    public interface Control
    {
        public void run(List<TaskSource> taskSources);
    }

    public static void transaction(List<EncoderPlugin> EncoderPlugins, List<ConfigSource> configs,
            Encoders.Control control)
    {
        // TODO
    }

    public static FileOutput open(List<EncoderPlugin> EncoderPlugins, List<TaskSource> taskSources,
            FileOutput output)
    {
        // TODO
    }
}
