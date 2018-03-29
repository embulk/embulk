package org.embulk.standards;

import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.AbstractReporterImpl;
import org.embulk.spi.ReporterPlugin;
import org.komamitsu.fluency.EventTime;
import org.komamitsu.fluency.Fluency;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Map;

public class FluentdReporterPlugin
        implements ReporterPlugin {
//    public interface PluginTask {
//        @Config("max_buffer_size")
//        String getMaxBufferSize();
//    }

    @Override
    public TaskSource configureTaskSource(final ConfigSource config) {
        return config.loadConfig(Task.class).dump();
    }

    @Override
    public AbstractReporterImpl open(final TaskSource taskSource) {
        this.fluentd = create(taskSource);
        return new FluentdReporterImpl();
    }

    @ThreadSafe
    private static class FluentdReporterImpl
            extends AbstractReporterImpl {
        @Override
        public void report(Level level, Map<String, Object> event) {
            // TODO
            String tag = "skip";
            // TODO
            EventTime eventTime = EventTime.fromEpochMilli(System.currentTimeMillis());
            try {
                fluentd.emit(tag, eventTime, event);
            } catch (IOException ex) {
                // TODO
            }
        }

        @Override
        public void close() {
            try {
                fluentd.flush();
                fluentd.close();
            } catch (IOException ex) {
                // TODO
            }
        }

        @Override
        public void cleanup() {
            fluentd.clearBackupFiles();
        }
    }

    private Fluency create(final TaskSource taskSource) {
        //PluginTask task = taskSource.loadTask(PluginTask.class);
        Fluency fluency = null;
        try {
            fluency = Fluency.defaultFluency(
                new Fluency.Config()
                        .setBufferChunkInitialSize(4 * 1024 * 1024)
                        .setBufferChunkRetentionSize(16 * 1024 * 1024)
                        .setMaxBufferSize(256 * 1024 * 1024L)
            );
        } catch (IOException ex) {
            // TODO
        }
        return fluency;
    }

    private static Fluency fluentd;
}
