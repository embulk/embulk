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

package org.embulk.standards;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.Exec;
import org.embulk.spi.FileOutput;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.embulk.util.file.FileOutputOutputStream;
import org.embulk.util.file.OutputStreamFileOutput;

public class GzipFileEncoderPlugin implements EncoderPlugin {
    public interface PluginTask extends Task {
        @Config("level")
        @ConfigDefault("6")
        int getLevel();
    }

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public void transaction(ConfigSource config, EncoderPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, PluginTask.class);
        if (1 > task.getLevel() || task.getLevel() > 9) {
            throw new ConfigException("\"level\" must be in the range of 1 <= level <= 9.");
        }
        control.run(task.dump());
    }

    @Override
    public FileOutput open(TaskSource taskSource, final FileOutput fileOutput) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);

        final FileOutputOutputStream output = new FileOutputOutputStream(fileOutput, Exec.getBufferAllocator(), FileOutputOutputStream.CloseMode.FLUSH);

        return new OutputStreamFileOutput(new OutputStreamFileOutput.Provider() {
                public OutputStream openNext() throws IOException {
                    output.nextFile();
                    return new GZIPOutputStream(output) {
                        {
                            this.def.setLevel(task.getLevel());
                        }
                    };
                }

                public void finish() throws IOException {
                    fileOutput.finish();
                }

                public void close() throws IOException {
                    fileOutput.close();
                }
            });
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();
}
