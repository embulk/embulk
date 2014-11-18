package org.quickload.standards;

import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.spi.ExecTask;

public class GzipFileEncoderPlugin
        extends OutputStreamFileEncoderPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("level")
        @ConfigDefault("6")
        public int getLevel();
    }

    public TaskSource getFileEncoderTask(ExecTask exec, ConfigSource config)
    {
        PluginTask task = exec.loadConfig(config, PluginTask.class);
        return exec.dumpTask(task);
    }

    @Override
    public OutputStream openOutputStream(ExecTask exec, TaskSource task,
            OutputStream out) throws IOException
    {
        // TODO GZIPOutputStream doesn't support level option?
        return new GZIPOutputStream(out);
    }
}
