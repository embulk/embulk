package org.quickload.standards;

import javax.validation.constraints.NotNull;
import org.quickload.buffer.Buffer;
import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.channel.FileBufferInput;
import org.quickload.spi.FileOutputPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ProcControl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class LocalFileOutputPlugin
        extends FileOutputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("out:paths")
        @NotNull
        public List<String> getPaths(); // TODO temporarily

        @Config("out:compress_type")
        public String getCompressType();
    }

    @Override
    public NextConfig runFileOutputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);

        control.run(proc.dumpTask(task));

        return new NextConfig();
    }

    @Override
    public Report runFileOutput(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput)
    {
        PluginTask task = proc.loadTask(taskSource, PluginTask.class);

        while (fileBufferInput.nextFile()) {
            for (Buffer buffer : fileBufferInput) {
                // TODO simple implementation

                String filePath = task.getPaths().get(processorIndex);
                File file = new File(filePath);
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try (OutputStream out = createFileOutputStream(file, task)) {
                    ByteBuffer buf = buffer.getBuffer();
                    byte[] bytes = buf.array(); // TODO
                    out.write(bytes, 0, bytes.length);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("write file: " + filePath); // TODO debug message
            }
        }

        return new Report();
    }

    private OutputStream createFileOutputStream(File file, PluginTask task)
        throws IOException
    {
        String compressType = task.getCompressType();
        if (compressType == null) { // null is for 'none' mode
            return new BufferedOutputStream(new FileOutputStream(file));
        } else if (compressType.equals("none")) { // TODO
            return new BufferedOutputStream(new FileOutputStream(file));
        } else if (compressType.equals("gzip")) {
            return new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        } else {
            throw new IOException("not supported yet");
        }
    }
}
