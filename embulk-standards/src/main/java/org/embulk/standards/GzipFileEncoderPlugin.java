package org.embulk.standards;

import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import org.embulk.config.Task;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.FileOutput;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.util.FileOutputOutputStream;
import org.embulk.spi.util.OutputStreamFileOutput;

public class GzipFileEncoderPlugin
        implements EncoderPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("level")
        @ConfigDefault("6")
        @Min(0)
        @Max(9)
        int getLevel();

        @ConfigInject
        BufferAllocator getBufferAllocator();
    }

    public void transaction(ConfigSource config, EncoderPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump());
    }

    @Override
    public FileOutput open(TaskSource taskSource, final FileOutput fileOutput)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);

        final FileOutputOutputStream output = new FileOutputOutputStream(fileOutput, task.getBufferAllocator(), FileOutputOutputStream.CloseMode.FLUSH);

        return new OutputStreamFileOutput(new OutputStreamFileOutput.Provider() {
            private GZIPOutputStream gzos;

            public OutputStream openNext() throws IOException
            {
                output.nextFile();
                gzos = new GZIPOutputStream(output) {
                    {
                        this.def.setLevel(task.getLevel());
                    }
                };
                return gzos;
            }

            public void finish() throws IOException
            {
                fileOutput.finish();
            }

            public void close() throws IOException
            {
                fileOutput.close();
                gzos.close();
            }
        });
    }
}
