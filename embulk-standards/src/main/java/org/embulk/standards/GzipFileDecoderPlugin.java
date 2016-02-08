package org.embulk.standards;

import java.io.InputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigInject;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.FileInput;
import org.embulk.spi.util.FileInputInputStream;
import org.embulk.spi.util.InputStreamFileInput;

public class GzipFileDecoderPlugin
        implements DecoderPlugin
{
    public interface PluginTask
            extends Task
    {
        @ConfigInject
        BufferAllocator getBufferAllocator();
    }

    @Override
    public void transaction(ConfigSource config, DecoderPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
    }

    @Override
    public FileInput open(TaskSource taskSource, FileInput fileInput)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        final FileInputInputStream files = new FileInputInputStream(fileInput);
        return new InputStreamFileInput(
                task.getBufferAllocator(),
                new InputStreamFileInput.Provider() {
                    private GZIPInputStream gzis;

                    public InputStream openNext() throws IOException
                    {
                        if (!files.nextFile()) {
                            return null;
                        }
                        gzis = new GZIPInputStream(files, 8*1024);
                        return gzis;
                    }

                    public void close() throws IOException
                    {
                        files.close();
                        gzis.close();
                    }
                });
    }
}
