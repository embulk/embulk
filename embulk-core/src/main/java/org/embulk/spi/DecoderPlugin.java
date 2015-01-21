package org.embulk.spi;

import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;

public interface DecoderPlugin
{
    public interface Control
    {
        public void run(TaskSource taskSource);
    }

    public void transaction(ConfigSource config, DecoderPlugin.Control control);

    public FileInput open(TaskSource taskSource, FileInput input);
}
