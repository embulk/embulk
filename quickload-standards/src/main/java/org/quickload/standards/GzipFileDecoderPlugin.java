package org.quickload.standards;

import java.io.InputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.spi.ProcTask;

public class GzipFileDecoderPlugin
        extends InputStreamFileDecoderPlugin
{
    public interface PluginTask
            extends Task
    {
    }

    public TaskSource getFileDecoderTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);
        return proc.dumpTask(task);
    }

    @Override
    public InputStream openInputStream(ProcTask proc, TaskSource taskSource,
            InputStream in) throws IOException
    {
        return new GZIPInputStream(in);
    }
}
