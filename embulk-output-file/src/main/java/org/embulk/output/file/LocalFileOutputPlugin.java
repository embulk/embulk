/*
 * Copyright 2014 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.output.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.FileOutputPlugin;
import org.embulk.spi.TransactionalFileOutput;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalFileOutputPlugin implements FileOutputPlugin {
    public interface PluginTask extends Task {
        @Config("path_prefix")
        String getPathPrefix();

        @Config("file_ext")
        String getFileNameExtension();

        @Config("sequence_format")
        @ConfigDefault("\"%03d.%02d.\"")
        String getSequenceFormat();
    }

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public ConfigDiff transaction(ConfigSource config, int taskCount,
            FileOutputPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, PluginTask.class);

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
            FileOutputPlugin.Control control) {
        control.run(taskSource);
        return CONFIG_MAPPER_FACTORY.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource,
            int taskCount,
            List<TaskReport> successTaskReports) {}

    @Override
    public TransactionalFileOutput open(TaskSource taskSource, final int taskIndex) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);

        final String pathPrefix = task.getPathPrefix();
        final String pathSuffix = task.getFileNameExtension();
        final String sequenceFormat = task.getSequenceFormat();

        return new TransactionalFileOutput() {
            private final List<String> fileNames = new ArrayList<>();
            private int fileIndex = 0;
            private FileOutputStream output = null;

            public void nextFile() {
                closeFile();
                String path = pathPrefix + String.format(sequenceFormat, taskIndex, fileIndex) + pathSuffix;
                logger.info("Writing local file '{}'", path);
                fileNames.add(path);
                try {
                    output = new FileOutputStream(new File(path));
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);  // TODO exception class
                }
                fileIndex++;
            }

            private void closeFile() {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            @SuppressWarnings("deprecation")  // Calling Buffer#array().
            public void add(Buffer buffer) {
                try {
                    output.write(buffer.array(), buffer.offset(), buffer.limit());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    buffer.release();
                }
            }

            public void finish() {
                closeFile();
            }

            public void close() {
                closeFile();
            }

            public void abort() {}

            public TaskReport commit() {
                TaskReport report = CONFIG_MAPPER_FACTORY.newTaskReport();
                // TODO better setting for Report
                // report.set("file_names", fileNames);
                // report.set("file_sizes", fileSizes);
                return report;
            }
        };
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();

    private static final Logger logger = LoggerFactory.getLogger(LocalFileOutputPlugin.class);
}
