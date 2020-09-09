package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;

public interface FormatterPlugin {
    interface Control {
        void run(TaskSource taskSource);
    }

    void transaction(ConfigSource config, Schema schema,
            FormatterPlugin.Control control);

    PageOutput open(TaskSource taskSource, Schema schema,
            FileOutput output);
}
