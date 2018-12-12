package org.embulk.standards;

import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.FileInput;
import org.embulk.spi.util.FileInputInputStream;
import org.embulk.spi.util.InputStreamFileInput;
import org.embulk.spi.util.InputStreamFileInput.InputStreamWithHints;

public class GzipFileDecoderPlugin implements DecoderPlugin {
    public interface PluginTask extends Task {
        @ConfigInject
        BufferAllocator getBufferAllocator();
    }

    @Override
    public void transaction(ConfigSource config, DecoderPlugin.Control control) {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
    }

    @Override
    public FileInput open(TaskSource taskSource, FileInput fileInput) {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        final FileInputInputStream files = new FileInputInputStream(fileInput);
        return new InputStreamFileInput(
                task.getBufferAllocator(),
                new InputStreamFileInput.Provider() {
                    // Implement openNextWithHints() instead of openNext() to show file name at parser plugin loaded by FileInputPlugin
                    // Because when using decoder, parser plugin can't get file name.
                    @Override
                    public InputStreamWithHints openNextWithHints() throws IOException {
                        if (!files.nextFile()) {
                            return null;
                        }
                        return new InputStreamWithHints(
                                new GZIPInputStream(files, 8 * 1024),
                                fileInput.hintOfCurrentInputFileNameForLogging().orElse(null)
                        );
                    }

                    @Override
                    public void close() throws IOException {
                        files.close();
                    }
                });
    }
}
