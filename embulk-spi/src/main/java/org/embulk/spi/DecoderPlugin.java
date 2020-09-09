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

package org.embulk.spi;

import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;

/**
 * The main class that a Decoder Plugin implements.
 *
 * <p>A Decoder Plugin converts a set of byte buffers in {@link org.embulk.spi.FileInput} to another set of byte buffers of
 * {@link org.embulk.spi.FileInput}.
 */
public interface DecoderPlugin {
    /**
     * A controller of the following tasks provided from the Embulk core.
     */
    interface Control {
        /**
         * Runs the following tasks of the Decoder Plugin.
         *
         * <p>It would be executed at the end of {@link #transaction(org.embulk.config.ConfigSource, DecoderPlugin.Control)}.
         *
         * @param taskSource  {@link org.embulk.config.TaskSource} processed for tasks from {@link org.embulk.config.ConfigSource}
         */
        void run(TaskSource taskSource);
    }

    /**
     * Processes the entire decoding transaction.
     *
     * @param config  a configuration for the Decoder Plugin given from a user
     * @param control  a controller of the following tasks provided from the Embulk core
     */
    void transaction(ConfigSource config, DecoderPlugin.Control control);

    /**
     * Opens a {@link org.embulk.spi.FileInput} instance that receives {@link org.embulk.spi.Buffer}s from a File Input Plugin,
     * or another Decoder Plugin, so that decoded input is read from a Parser Plugin, or another Decoder Plugin
     *
     * <p>It processes each decoding task.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param fileInput  {@link org.embulk.spi.FileInput} to read byte buffers from a File Input Plugin, or another Decoder Plugin
     * @return an implementation of {@link org.embulk.spi.FileInput} that receives {@link org.embulk.spi.Buffer}s from a File
     *     Input Plugin, or another Decoder Plugin, so that decoded input is read from a Parser Plugin, or another Decoder Plugin
     */
    FileInput open(TaskSource taskSource, FileInput fileInput);
}
