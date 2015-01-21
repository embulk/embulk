package org.embulk.standards;

import java.io.InputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import com.fasterxml.jackson.annotation.JacksonInject;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.FileInput;
import org.embulk.spi.FileInputInputStream;
import org.embulk.spi.InputStreamFileInput;

public class GzipFileDecoderPlugin
        implements DecoderPlugin
{
    public interface PluginTask
            extends Task
    {
        @JacksonInject
        public BufferAllocator getBufferAllocator();
    }

    @Override
    public void transaction(ConfigSource config, DecoderPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
    }

    @Override
    public FileInput open(TaskSource taskSource, FileInput input)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        final FileInputInputStream files = new FileInputInputStream(input);
        return new InputStreamFileInput(
                task.getBufferAllocator(),
                new InputStreamFileInput.Provider() {
                    public InputStream openNext() throws IOException
                    {
                        if (!files.nextFile()) {
                            return null;
                        }
                        return new GZIPInputStream(files);
                    }

                    public void close() throws IOException
                    {
                        files.close();
                    }
                });
    }
}
