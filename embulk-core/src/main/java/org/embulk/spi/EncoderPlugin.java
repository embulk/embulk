package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;

public interface EncoderPlugin {
    interface Control {
        void run(TaskSource taskSource);
    }

    void transaction(ConfigSource config, EncoderPlugin.Control control);

    FileOutput open(TaskSource taskSource, FileOutput fileOutput);
}
