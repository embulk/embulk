package org.quickload.standards;

import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import org.quickload.config.Task;
import org.quickload.config.Config;
import org.quickload.config.ConfigDefault;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.spi.ProcTask;

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

    public TaskSource getFileEncoderTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);
        return proc.dumpTask(task);
    }

    @Override
    public OutputStream openOutputStream(ProcTask proc, TaskSource task,
            OutputStream out) throws IOException
    {
        // TODO GZIPOutputStream doesn't support level option?
        return new GZIPOutputStream(out);
    }
}
