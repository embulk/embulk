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
import com.fasterxml.jackson.annotation.JacksonInject;
import com.google.inject.Inject;
import org.embulk.config.Config;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.spi.util.InputStreamFileInput;
import org.slf4j.Logger;

import static org.embulk.spi.util.Inputs.formatPrefix;

public class LocalFileInputPlugin
        implements FileInputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("paths")
        @NotNull
        public List<String> getPathPrefixes();

        public List<String> getFiles();
        public void setFiles(List<String> files);

        @JacksonInject
        public BufferAllocator getBufferAllocator();
    }

    private final Logger log;

    @Inject
    public LocalFileInputPlugin()
    {
        this.log = Exec.getLogger(getClass());
    }

    @Override
    public NextConfig transaction(ConfigSource config, FileInputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        // list files recursively
        try {
            task.setFiles(listFiles(task));
        } catch (IOException e) {
            log.error("Cannot get a list of the files: "+e.getMessage());
            throw new RuntimeException(e);  // TODO exception class
        }

        // run with threads. number of processors is same with number of files
        control.run(task.dump(), task.getFiles().size());

        return Exec.newNextConfig();
    }

    public List<String> listFiles(PluginTask task) throws IOException
    {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (String prefix : task.getPathPrefixes()) {
            String formatted;
            try {
                formatted = formatPrefix(prefix);
            } catch (RuntimeException e) {
                //  The RuntimeException that is thrown during formatPath() call should
                //  be a fatal error. It should stop the input transaction and notice
                //  that the prefix includes an invalid syntax for users.
                log.error("Cannot parse the invalid prefix. An invalid syntax is included in: "+prefix);
                throw e;
            }
            Files.walkFileTree(Paths.get(formatted), new SimpleFileVisitor<Path>() {
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
    public TransactionalFileInput open(TaskSource taskSource, int processorIndex)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        return new LocalFileInput(task, processorIndex);
    }

    public static class LocalFileInput
            extends InputStreamFileInput
            implements TransactionalFileInput
    {
        // TODO create single-file InputStreamFileInput utility
        private static class SingleFileProvider
                implements InputStreamFileInput.Provider
        {
            private final File file;
            private boolean opened = false;

            public SingleFileProvider(File file)
            {
                this.file = file;
            }

            @Override
            public InputStream openNext() throws IOException
            {
                if (opened) {
                    return null;
                }
                opened = true;
                return new FileInputStream(file);
            }

            @Override
            public void close() { }
        }

        public LocalFileInput(PluginTask task, int processorIndex)
        {
            super(task.getBufferAllocator(), new SingleFileProvider(new File(task.getFiles().get(processorIndex))));
        }

        @Override
        public void abort() { }

        @Override
        public CommitReport commit()
        {
            return Exec.newCommitReport();
        }
    }
}
