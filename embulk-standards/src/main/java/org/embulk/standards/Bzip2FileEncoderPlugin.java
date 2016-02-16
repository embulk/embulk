package org.embulk.standards;

import java.io.OutputStream;
import java.io.IOException;
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
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

public class Bzip2FileEncoderPlugin
        implements EncoderPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("level")
        @ConfigDefault("9")
        @Min(1)
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
            public OutputStream openNext() throws IOException
            {
                output.nextFile();
                return new BZip2CompressorOutputStream(output, task.getLevel());
            }

            public void finish() throws IOException
            {
                fileOutput.finish();
            }

            public void close() throws IOException
            {
                fileOutput.close();
            }
        });
    }
}
