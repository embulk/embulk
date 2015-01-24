package org.embulk.standards;

import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.FileOutput;
import org.embulk.spi.util.FileOutputOutputStream;

public class GzipFileEncoderPlugin
        implements EncoderPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("level")
        @ConfigDefault("6")
        public int getLevel();
    }

    public void transaction(ConfigSource config, EncoderPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
    }

    @Override
    public FileOutput open(TaskSource taskSource, FileOutput fileOutput)
    {
        throw new AssertionError("OutputStreamFileOutput is not implemented yet");
        // TODO GZIPOutputStream doesn't support level option?
        //return new OutputStreamFileOutput(new GZIPOutputStream(new FileOutputOutputStream(fileOutput)));
    }
}
