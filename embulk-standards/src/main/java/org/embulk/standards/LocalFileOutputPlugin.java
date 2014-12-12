package org.embulk.standards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

import org.embulk.channel.FileBufferInput;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.BufferPlugins;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.ExecTask;
import org.embulk.spi.ExecControl;

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
    public NextConfig runFileOutputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control)
    {
        PluginTask task = exec.loadConfig(config, PluginTask.class);

        control.run(exec.dumpTask(task));

        return new NextConfig();
    }

    @Override
    public Report runFileOutput(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput)
    {
        PluginTask task = exec.loadTask(taskSource, PluginTask.class);

        // TODO format path using timestamp
        String fileName = task.getFileNameFormat();

        String pathPrefix = task.getDirectory() + File.separator + fileName;
        String pathSuffix = task.getFileNameExtension();

        int fileIndex = 0;
        List<String> fileNames = new ArrayList<>();
        List<Long> fileSizes = new ArrayList<>();
        while (fileBufferInput.nextFile()) {
            String path = pathPrefix + String.format(".%03d.%02d.", processorIndex, fileIndex) + pathSuffix;
            System.out.println("path: "+path);
            File file = new File(path);
            file.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(file)) {
                BufferPlugins.transferBufferInput(exec.getBufferAllocator(),
                        fileBufferInput, out);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.out.println("file written: "+path);
            fileNames.add(path);
            fileSizes.add(file.length());
            fileIndex++;
        }

        Report report = new Report();
        // TODO better setting for Report
        // report.set("file_names", fileNames);
        // report.set("file_sizes", fileSizes);
        return report;
    }
}
