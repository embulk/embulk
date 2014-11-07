package org.quickload.standards;

import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import javax.validation.constraints.NotNull;
import com.google.common.collect.ImmutableList;
import org.quickload.config.Config;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.config.NextConfig;
import org.quickload.config.Report;
import org.quickload.channel.FileBufferOutput;
import org.quickload.plugin.PluginManager;
import org.quickload.record.Schema;
import org.quickload.spi.FileInputPlugin;
import org.quickload.spi.ProcTask;
import org.quickload.spi.ProcControl;

public class LocalFileInputPlugin
        extends FileInputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("in:paths")
        @NotNull
        public List<String> getPathPrefixes();

        public List<String> getFiles();
        public void setFiles(List<String> files);
    }

    @Override
    public NextConfig runFileInputTransaction(ProcTask proc, ConfigSource config,
            ProcControl control)
    {
        PluginTask task = proc.loadConfig(config, PluginTask.class);

        // list files recursively
        try {
            task.setFiles(listFiles(task));
        } catch (IOException ex) {
            throw new RuntimeException(ex);  // TODO exception class
        }

        // number of processors is same with number of files
        proc.setProcessorCount(task.getFiles().size());

        // run
        control.run(proc.dumpTask(task));

        return new NextConfig();
    }

    public List<String> listFiles(PluginTask task) throws IOException
    {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (String prefix : task.getPathPrefixes()) {
            // TODO format path using timestamp
            Files.walkFileTree(Paths.get(prefix), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes aAttrs)
                {
                    builder.add(file.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return builder.build();
    }

    @Override
    public Report runFileInput(ProcTask proc, TaskSource taskSource,
            int processorIndex, FileBufferOutput fileBufferOutput)
    {
        PluginTask task = proc.loadTask(taskSource, PluginTask.class);

        String path = task.getFiles().get(processorIndex);
        File file = new File(path);

        try (InputStream in = new FileInputStream(file)) {
            FilePlugins.transferInputStream(proc.getBufferAllocator(),
                    in, fileBufferOutput);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return new Report();
    }
}
