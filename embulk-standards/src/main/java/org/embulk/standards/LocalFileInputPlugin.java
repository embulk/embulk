package org.embulk.standards;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
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
import com.google.common.collect.ImmutableList;
import com.google.common.base.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigDefault;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskReport;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.spi.util.InputStreamTransactionalFileInput;
import org.slf4j.Logger;

public class LocalFileInputPlugin
        implements FileInputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("path_prefix")
        String getPathPrefix();

        @Config("last_path")
        @ConfigDefault("null")
        Optional<String> getLastPath();

        List<String> getFiles();
        void setFiles(List<String> files);

        @ConfigInject
        BufferAllocator getBufferAllocator();
    }

    private final Logger log = Exec.getLogger(getClass());

    private final static Path CURRENT_DIR = Paths.get(".").normalize();

    @Override
    public ConfigDiff transaction(ConfigSource config, FileInputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        // list files recursively
        List<String> files = listFiles(task);
        log.info("Loading files {}", files);
        task.setFiles(files);

        // number of processors is same with number of files
        int taskCount = task.getFiles().size();
        return resume(task.dump(), taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            int taskCount,
            FileInputPlugin.Control control)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);

        control.run(taskSource, taskCount);

        // build next config
        ConfigDiff configDiff = Exec.newConfigDiff();

        // last_path
        if (task.getFiles().isEmpty()) {
            // keep the last value
            if (task.getLastPath().isPresent()) {
                configDiff.set("last_path", task.getLastPath().get());
            }
        } else {
            List<String> files = new ArrayList<String>(task.getFiles());
            Collections.sort(files);
            configDiff.set("last_path", files.get(files.size() - 1));
        }

        return configDiff;
    }

    @Override
    public void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports)
    { }

    public List<String> listFiles(PluginTask task)
    {
        Path pathPrefix = Paths.get(task.getPathPrefix()).normalize();
        final Path directory;
        final String fileNamePrefix;
        if (Files.isDirectory(pathPrefix)) {
            directory = pathPrefix;
            fileNamePrefix = "";
        } else {
            fileNamePrefix = pathPrefix.getFileName().toString();
            Path d = pathPrefix.getParent();
            directory = (d == null ? CURRENT_DIR : d);
        }

        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        final String lastPath = task.getLastPath().orNull();
        try {
            log.info("Listing local files at directory '{}' filtering filename by prefix '{}'", directory.equals(CURRENT_DIR) ? "." : directory.toString(), fileNamePrefix);
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs)
                {
                    if (path.equals(directory)) {
                        return FileVisitResult.CONTINUE;
                    } else if (lastPath != null && path.toString().compareTo(lastPath) <= 0) {
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        Path parent = path.getParent();
                        if (parent == null) {
                            parent = CURRENT_DIR;
                        }
                        if (parent.equals(directory)) {
                            if (path.getFileName().toString().startsWith(fileNamePrefix)) {
                                return FileVisitResult.CONTINUE;
                            } else {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        } else {
                            return FileVisitResult.CONTINUE;
                        }
                    }
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                {
                    if (lastPath != null && path.toString().compareTo(lastPath) <= 0) {
                        return FileVisitResult.CONTINUE;
                    } else {
                        Path parent = path.getParent();
                        if (parent == null) {
                            parent = CURRENT_DIR;
                        }
                        if (parent.equals(directory)) {
                            if (path.getFileName().toString().startsWith(fileNamePrefix)) {
                                builder.add(path.toString());
                                return FileVisitResult.CONTINUE;
                            }
                        } else {
                            builder.add(path.toString());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }
            });
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Failed get a list of local files at '%s'", directory), ex);
        }
        return builder.build();
    }

    @Override
    public TransactionalFileInput open(TaskSource taskSource, int taskIndex)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);

        final File file = new File(task.getFiles().get(taskIndex));

        return new InputStreamTransactionalFileInput(
                task.getBufferAllocator(),
                new InputStreamTransactionalFileInput.Opener() {
                    public InputStream open() throws IOException
                    {
                        return new FileInputStream(file);
                    }
                })
        {
            @Override
            public void abort()
            { }

            @Override
            public TaskReport commit()
            {
                return Exec.newTaskReport();
            }
        };
    }
}
