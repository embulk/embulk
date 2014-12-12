package org.embulk.standards;

import java.io.InputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.spi.ExecTask;

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
