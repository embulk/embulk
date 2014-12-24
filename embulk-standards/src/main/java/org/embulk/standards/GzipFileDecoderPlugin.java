package org.embulk.standards;

import java.io.InputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.FileInput;
import org.embulk.spi.FileInputInputStream;
import org.embulk.spi.InputStreamFileInput;

public class GzipFileDecoderPlugin
        implements DecoderPlugin
{
    public interface PluginTask
            extends Task
    {
    }

    @Override
    public void transaction(ConfigSource config, DecoderPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        contro.run(task.dump());
    }

    @Override
    public FileInput open(TaskSource taskSource, FileInput input)
    {
        return new InputStreamFileInput(new GZIPInputStream(new FileInputInputStream(input)));
    }
}
