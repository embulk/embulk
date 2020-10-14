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
 * The main class that an Encoder Plugin implements.
 *
 * <p>An Encoder Plugin converts a set of byte buffers in {@link org.embulk.spi.FileOutput} to another set of byte buffers of
 * {@link org.embulk.spi.FileOutput}.
 *
 * @since 0.4.0
 */
public interface EncoderPlugin {
    /**
     * A controller of the following tasks provided from the Embulk core.
     *
     * @since 0.4.0
     */
    interface Control {
        /**
         * Runs the following tasks of the Encoder Plugin.
         *
         * <p>It would be executed at the end of {@link #transaction(org.embulk.config.ConfigSource, EncoderPlugin.Control)}.
         *
         * @param taskSource  {@link org.embulk.config.TaskSource} processed for tasks from {@link org.embulk.config.ConfigSource}
         *
         * @since 0.4.0
         */
        void run(TaskSource taskSource);
    }

    /**
     * Processes the entire encoding transaction.
     *
     * @param config  a configuration for the Encoder Plugin given from a user
     * @param control  a controller of the following tasks provided from the Embulk core
     *
     * @since 0.4.0
     */
    void transaction(ConfigSource config, EncoderPlugin.Control control);

    /**
     * Opens a {@link org.embulk.spi.FileOutput} instance that receives {@link org.embulk.spi.Buffer}s from a Formatter Plugin,
     * or another Encoder Plugin, and writes encoded output into {@link org.embulk.spi.FileOutput} {@code fileOutput} in the
     * parameter list.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param fileOutput  {@link org.embulk.spi.FileOutput} to write encoded output for a File Output Plugin, or another Encoder
     *     Plugin
     * @return an implementation of {@link org.embulk.spi.FileOutput} that receives {@link org.embulk.spi.Buffer}s from a Formatter
     *     Plugin, or another Encoder Plugin, and writes encoded output into {@link org.embulk.spi.FileOutput} {@code fileOutput}
     *     in the parameter list
     *
     * @since 0.4.0
     */
    FileOutput open(TaskSource taskSource, FileOutput fileOutput);
}
