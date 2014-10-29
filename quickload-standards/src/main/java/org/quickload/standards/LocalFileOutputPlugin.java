package org.quickload.standards;

import javax.validation.constraints.NotNull;
import org.quickload.buffer.Buffer;
import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.Report;
import org.quickload.config.NullReport;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.channel.BufferInput;
import org.quickload.spi.FileOutputPlugin;
import org.quickload.spi.ProcTask;

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
    public TaskSource getFileOutputTask(ProcTask proc, ConfigSource config)
    {
        PluginTask task = config.loadTask(PluginTask.class);
        return config.dumpTask(task);
    }

    @Override
    public Report runFileOutput(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            BufferInput bufferInput)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        for (Buffer buffer : bufferInput) {
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

        return new NullReport();
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
