package org.quickload.standards;

import java.io.InputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.spi.ExecTask;

public class GzipFileDecoderPlugin
        extends InputStreamFileDecoderPlugin
{
    public interface PluginTask
            extends Task
    {
    }

    public TaskSource getFileDecoderTask(ExecTask exec, ConfigSource config)
    {
        PluginTask task = exec.loadConfig(config, PluginTask.class);
        return exec.dumpTask(task);
    }

    @Override
    public InputStream openInputStream(ExecTask exec, TaskSource taskSource,
            InputStream in) throws IOException
    {
        return new GZIPInputStream(in);
    }
}
