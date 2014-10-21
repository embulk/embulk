package org.quickload.standards;

import javax.validation.constraints.NotNull;
import com.google.inject.Inject;
import org.quickload.buffer.Buffer;
import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.spi.BufferOperator;
import org.quickload.spi.FileOutputPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.Report;
import org.quickload.spi.FailedReport;

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
    @Inject
    public LocalFileOutputPlugin(PluginManager pluginManager)
    {
        super(pluginManager);
    }

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
    public BufferOperator openBufferOutputOperator(ProcTask proc,
            TaskSource taskSource, int processorIndex)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        return new PluginOperator(task, processorIndex);
    }

    // TODO can be PluginOperator ported to standard library?
    public static class PluginOperator
            implements BufferOperator
    {
        private final PluginTask task;
        private final int processorIndex;

        PluginOperator(PluginTask task, int processorIndex)
        {
            this.task = task;
            this.processorIndex = processorIndex;
        }

        @Override
        public void addBuffer(Buffer buffer) {
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

            try (OutputStream out = createFileOutputStream(file)) {
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

        private OutputStream createFileOutputStream(File file)
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

        @Override
        public Report failed(Exception cause)
        {
            // TODO
            return new FailedReport(null, null);
        }

        @Override
        public Report completed() {
            return null; // TODO
        }

        @Override
        public void close() throws Exception {
            // TODO
        }
    }
}
