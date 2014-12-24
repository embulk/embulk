package org.embulk.standards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;
import org.embulk.config.CommitReport;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.TransactionalFileOutput;
import org.embulk.spi.Exec;

public class LocalFileOutputPlugin
        implements FileOutputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("directory")
        public String getDirectory();

        @Config("file_name")
        public String getFileNameFormat();

        @Config("file_ext")
        public String getFileNameExtension();

        // TODO support in FileInputPlugin and FileOutputPlugin
        //@Config("compress_type")
        //public String getCompressType();
    }

    @Override
    public NextConfig transaction(ConfigSource config, int processorCount,
            FileOutputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        control.run(task.dump());

        return Exec.newNextConfig();
    }

    @Override
    public TransactionalFileOutput open(TaskSource taskSource, final int processorIndex)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);

        // TODO format path using timestamp
        final String fileName = task.getFileNameFormat();

        final String pathPrefix = task.getDirectory() + File.separator + fileName;
        final String pathSuffix = task.getFileNameExtension();

        final List<String> fileNames = new ArrayList<>();

        return new TransactionalFileOutput() {
            private int fileIndex = 0;
            private FileOutputStream output = null;

            public void nextFile()
            {
                closeFile();
                String path = pathPrefix + String.format(".%03d.%02d.", processorIndex, fileIndex) + pathSuffix;
                System.out.println("path: "+path);  // TODO use Exec.getLogger()
                fileNames.add(path);
                output = new FileOutputStream(new File(path));
                fileIndex++;
            }

            private void closeFile()
            {
                if (output != null) {
                    System.out.println("file written");
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
                    output.write(buffer.array(), buffer.offset(), buffer.size());
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
