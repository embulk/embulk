package org.embulk.exec;

import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.AbstractReporterImpl;
import org.embulk.spi.ReporterPlugin;

// TODO should describe why we need to implement this plugin here.
public class StdoutReporterPlugin
        implements ReporterPlugin {
    @Override
    public TaskSource configureTaskSource(final ConfigSource config) {
        return config.loadConfig(Task.class).dump();
    }

    @Override
    public AbstractReporterImpl open(final TaskSource task) {
        return new StdoutReporterImpl();
    }

    @ThreadSafe
    private static class StdoutReporterImpl
            extends AbstractReporterImpl {
        @Override
        public void report(Level level, Map<String, Object> event) {
            System.out.println(event);
        }

        @Override
        public void close() {
        }

        @Override
        public void cleanup() {
        }
    }
}
