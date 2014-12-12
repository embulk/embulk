package org.embulk.standards;

import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.spi.ExecTask;

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
