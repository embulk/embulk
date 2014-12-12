package org.embulk.standards;

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
import org.embulk.config.Config;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.Report;
import org.embulk.channel.FileBufferOutput;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.FilePlugins;
import org.embulk.spi.ExecTask;
import org.embulk.spi.ExecControl;

public class LocalFileInputPlugin
        extends FileInputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("paths")
        @NotNull
        public List<String> getPathPrefixes();

        public List<String> getFiles();
        public void setFiles(List<String> files);
    }

    @Override
    public NextConfig runFileInputTransaction(ExecTask exec, ConfigSource config,
            ExecControl control)
    {
        PluginTask task = exec.loadConfig(config, PluginTask.class);

        // list files recursively
        try {
            task.setFiles(listFiles(task));
        } catch (IOException ex) {
            throw new RuntimeException(ex);  // TODO exception class
        }

        // number of processors is same with number of files
        exec.setProcessorCount(task.getFiles().size());

        // run
        control.run(exec.dumpTask(task));

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
    public Report runFileInput(ExecTask exec, TaskSource taskSource,
            int processorIndex, FileBufferOutput fileBufferOutput)
    {
        PluginTask task = exec.loadTask(taskSource, PluginTask.class);

        String path = task.getFiles().get(processorIndex);
        File file = new File(path);

        try (InputStream in = new FileInputStream(file)) {
            FilePlugins.transferInputStream(exec.getBufferAllocator(),
                    in, fileBufferOutput);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return new Report();
    }
}
