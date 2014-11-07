package org.quickload.standards;

import java.io.OutputStream;
import java.io.IOException;
import org.quickload.config.Task;
import java.util.zip.GZIPOutputStream;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.spi.ProcTask;

public class GzipFileEncoderPlugin
        extends OutputStreamFileEncoderPlugin
{
    public interface PluginTask
            extends Task
    {
    }

    public TaskSource getFileEncoderTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);
        return proc.dumpTask(task);
    }

    @Override
    public OutputStream openOutputStream(OutputStream out) throws IOException
    {
        return new GZIPOutputStream(out);
    }
}
