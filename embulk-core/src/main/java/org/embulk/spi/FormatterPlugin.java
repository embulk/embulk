package org.embulk.spi;

import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.type.Schema;

public interface FormatterPlugin
{
    public interface Control
    {
        public void run(TaskSource taskSource);
    }

    public void transaction(ConfigSource config, Schema schema,
            FormatterPlugin.Control control);

    public PageOutput open(TaskSource taskSource, Schema schema,
            FileOutput output);
}
