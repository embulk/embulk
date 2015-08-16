package org.embulk.standards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.IllegalFormatException;
import org.embulk.config.Config;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.TaskReport;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.TransactionalFileOutput;
import org.embulk.spi.Exec;
import org.slf4j.Logger;

public class LocalFileOutputPlugin
        implements FileOutputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("path_prefix")
        String getPathPrefix();

        @Config("file_ext")
        String getFileNameExtension();

        @Config("sequence_format")
        @ConfigDefault("\"%03d.%02d.\"")
        String getSequenceFormat();
    }

    private final Logger log = Exec.getLogger(getClass());

    @Override
    public ConfigDiff transaction(ConfigSource config, int taskCount,
            FileOutputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        // validate sequence_format
        try {
            String dontCare = String.format(Locale.ENGLISH, task.getSequenceFormat(), 0, 0);
        } catch (IllegalFormatException ex) {
            throw new ConfigException("Invalid sequence_format: parameter for file output plugin", ex);
        }

        return resume(task.dump(), taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            int taskCount,
            FileOutputPlugin.Control control)
    {
        control.run(taskSource);
        return Exec.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports)
    { }

    @Override
    public TransactionalFileOutput open(TaskSource taskSource, final int taskIndex)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);

        final String pathPrefix = task.getPathPrefix();
        final String pathSuffix = task.getFileNameExtension();
        final String sequenceFormat = task.getSequenceFormat();

        return new TransactionalFileOutput() {
            private final List<String> fileNames = new ArrayList<>();
            private int fileIndex = 0;
            private FileOutputStream output = null;

            public void nextFile()
            {
                closeFile();
                String path = pathPrefix + String.format(sequenceFormat, taskIndex, fileIndex) + pathSuffix;
                log.info("Writing local file '{}'", path);
                fileNames.add(path);
                try {
                    output = new FileOutputStream(new File(path));
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);  // TODO exception class
                }
                fileIndex++;
            }

            private void closeFile()
            {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            public void add(Buffer buffer)
            {
                try {
                    output.write(buffer.array(), buffer.offset(), buffer.limit());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    buffer.release();
                }
            }

            public void finish()
            {
                closeFile();
            }

            public void close()
            {
                closeFile();
            }

            public void abort()
            { }

            public TaskReport commit()
            {
                TaskReport report = Exec.newTaskReport();
                // TODO better setting for Report
                // report.set("file_names", fileNames);
                // report.set("file_sizes", fileSizes);
                return report;
            }
        };
    }
}
