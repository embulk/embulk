package org.embulk.spi;

import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;

public interface ParserPlugin
{
    public interface Control
    {
        public void run(TaskSource taskSource, Schema schema);
    }

    public void transaction(ConfigSource config, ParserPlugin.Control control);

    public void run(TaskSource taskSource, Schema schema,
            FileInput input, PageOutput output);
}
