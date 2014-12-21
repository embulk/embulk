package org.embulk.spi;

import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;

public interface EncoderPlugin
{
    public interface Control
    {
        public void run(TaskSource taskSource);
    }

    public void configure(ConfigSource config, EncoderPlugin.Control control);

    public FileOutput open(TaskSource taskSource, FileOutput fileOutput);
}
