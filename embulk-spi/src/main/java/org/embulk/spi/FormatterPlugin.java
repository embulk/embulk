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
 * The main class that a Formatter Plugin implements.
 *
 * <p>A Formatter Plugin formats a sequence of data records in {@link org.embulk.spi.PageOutput} from an Input Plugin, or a Filter
 * Plugin, into a set of byte buffers of {@link org.embulk.spi.FileOutput} for a File Output Plugin, or an Encoder Plugin.
 */
public interface FormatterPlugin {
    /**
     * A controller of the following tasks provided from the Embulk core.
     */
    interface Control {
        /**
         * Runs the following tasks of the Formatter Plugin.
         *
         * <p>It would be executed at the end of
         * {@link #transaction(org.embulk.config.ConfigSource, org.embulk.spi.Schema, FormatterPlugin.Control)}.
         *
         * @param taskSource  {@link org.embulk.config.TaskSource} processed for tasks from {@link org.embulk.config.ConfigSource}
         */
        void run(TaskSource taskSource);
    }

    /**
     * Processes the entire formatting transaction.
     *
     * @param config  a configuration for the Formatter Plugin given from a user
     * @param schema  {@link org.embulk.spi.Schema} of the input for the formatter
     * @param control  a controller of the following tasks provided from the Embulk core
     */
    void transaction(ConfigSource config, Schema schema, FormatterPlugin.Control control);

    /**
     * Opens a {@link org.embulk.spi.PageOutput} instance that receives {@link org.embulk.spi.Page}s from an Input Plugin, or
     * a Filter Plugin, and writes formatted output into {@link org.embulk.spi.FileOutput} {@code output} in the parameter list
     * for a File Output Plugin, or an Encoder Plugin.
     *
     * <p>It processes each formatting task.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param schema  {@link org.embulk.spi.Schema} of the input for the formatter
     * @param fileOutput  {@link org.embulk.spi.FileOutput} to write formatted output so that the output is read from a File
     *     Output Plugin, or an Encoder Plugin
     * @return an implementation of {@link org.embulk.spi.PageOutput} that receives {@link org.embulk.spi.Page}s from an Input
     *     Plugin, or a Filter Plugin, and writes formatted output into {@link org.embulk.spi.FileOutput} {@code output} in the
     *     parameter list for a File Output Plugin, or an Encoder Plugin
     */
    PageOutput open(TaskSource taskSource, Schema schema, FileOutput output);
}
