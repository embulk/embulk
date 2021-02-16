/*
 * Copyright 2016 The Embulk project
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
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.DecoderPlugin;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInput;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.embulk.util.file.FileInputInputStream;
import org.embulk.util.file.InputStreamFileInput;

public class Bzip2FileDecoderPlugin implements DecoderPlugin {
    public interface PluginTask extends Task {}

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public void transaction(ConfigSource config, DecoderPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, PluginTask.class);
        control.run(task.dump());
    }

    @Override
    public FileInput open(TaskSource taskSource, FileInput fileInput) {
        final FileInputInputStream files = new FileInputInputStream(fileInput);
        return new InputStreamFileInput(
                Exec.getBufferAllocator(),
                new InputStreamFileInput.Provider() {
                    // Implement openNextWithHints() instead of openNext() to show file name at parser plugin loaded by FileInputPlugin
                    // Because when using decoder, parser plugin can't get file name.
                    @Override
                    public InputStreamFileInput.InputStreamWithHints openNextWithHints() throws IOException {
                        if (!files.nextFile()) {
                            return null;
                        }
                        return new InputStreamFileInput.InputStreamWithHints(
                                new BZip2CompressorInputStream(files, true),
                                fileInput.hintOfCurrentInputFileNameForLogging().orElse(null)
                        );
                    }

                    @Override
                    public void close() throws IOException {
                        files.close();
                    }
                });
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();
}
