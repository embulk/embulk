package org.embulk.standards;

import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.AbstractReporterImpl;
import org.embulk.spi.ReporterPlugin;

public class NullReporterPlugin
        implements ReporterPlugin {
    @Override
    public TaskSource configureTaskSource(final ConfigSource config) {
        return config.loadConfig(Task.class).dump();
    }

    @Override
    public AbstractReporterImpl open(final TaskSource task) {
        return new NullReporterImpl();
    }

    @ThreadSafe
    public static class NullReporterImpl
            extends AbstractReporterImpl {
        @Override
        public void report(Level level, Map<String, Object> event) { }

        @Override
        public void close() { }

        @Override
        public void cleanup() { }
    }
}
