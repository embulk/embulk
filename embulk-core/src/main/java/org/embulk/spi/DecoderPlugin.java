package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;

public interface DecoderPlugin {
    interface Control {
        void run(TaskSource taskSource);
    }

    void transaction(ConfigSource config, DecoderPlugin.Control control);

    FileInput open(TaskSource taskSource, FileInput input);
}
