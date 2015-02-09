package org.embulk.standards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.ConfigDiff;
import org.embulk.config.CommitReport;
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
        public String getPathPrefix();

        @Config("file_ext")
        public String getFileNameExtension();

        @Config("sequence_format")
        @ConfigDefault("\".%03d.%02d\"")
        public String getSequenceFormat();
    }

    private final Logger log = Exec.getLogger(getClass());

    @Override
    public ConfigDiff transaction(ConfigSource config, int processorCount,
            FileOutputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        // validate sequence_format
        String.format(task.getSequenceFormat(), 0, 0);

        return resume(task.dump(), processorCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            int processorCount,
            FileOutputPlugin.Control control)
    {
        control.run(taskSource);
        return Exec.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource,
            int processorCount,
            List<CommitReport> successCommitReports)
    { }

    @Override
    public TransactionalFileOutput open(TaskSource taskSource, final int processorIndex)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);

        final String pathPrefix = task.getPathPrefix();
        final String pathSuffix = task.getFileNameExtension();
        final String sequenceFormat = task.getSequenceFormat();

        final List<String> fileNames = new ArrayList<>();

        return new TransactionalFileOutput() {
            private int fileIndex = 0;
            private FileOutputStream output = null;

            public void nextFile()
            {
                closeFile();
                String path = pathPrefix + String.format(sequenceFormat, processorIndex, fileIndex) + pathSuffix;
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

            public void abort() { }

            public CommitReport commit()
            {
                CommitReport report = Exec.newCommitReport();
                // TODO better setting for Report
                // report.set("file_names", fileNames);
                // report.set("file_sizes", fileSizes);
                return report;
            }
        };
    }
}
