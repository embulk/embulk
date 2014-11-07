package org.quickload.standards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.channel.FileBufferInput;
import org.quickload.spi.BufferPlugins;
import org.quickload.spi.FileOutputPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ProcControl;

public class LocalFileOutputPlugin
        extends FileOutputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("directory")
        @NotNull
        public String getDirectory();

        @Config("file_name")
        @NotNull
        public String getFileNameFormat();

        @Config("file_ext")
        @NotNull
        public String getFileNameExtension();

        // TODO support in FileInputPlugin and FileOutputPlugin
        //@Config("compress_type")
        //public String getCompressType();
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

        // TODO format path using timestamp
        String fileName = task.getFileNameFormat();

        String pathPrefix = task.getDirectory() + File.separator + fileName;
        String pathSuffix = task.getFileNameExtension();

        int fileIndex = 0;
        while (fileBufferInput.nextFile()) {
            String path = pathPrefix + String.format(".%03d.%02d.", processorIndex, fileIndex) + pathSuffix;
            System.out.println("path: "+path);
            File file = new File(path);
            file.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(file)) {
                BufferPlugins.transferBufferInput(proc.getBufferAllocator(),
                        fileBufferInput, out);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.out.println("file written: "+path);
            fileIndex++;
        }

        return new Report();
    }
}
