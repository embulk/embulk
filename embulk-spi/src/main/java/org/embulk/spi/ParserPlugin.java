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

import java.util.Optional;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;

/**
 * The main class that a Parser Plugin implements.
 *
 * <p>A Parser Plugin parses a set of byte buffers in {@link org.embulk.spi.FileInput} from a File Input Plugin, or an Decoder
 * Plugin, so that parsed input is read from an Output Plugin, or a Filter Plugin.
 *
 * @since 0.4.0
 */
public interface ParserPlugin {
    /**
     * A controller of the following tasks provided from the Embulk core.
     *
     * @since 0.4.0
     */
    interface Control {
        /**
         * Runs the following tasks of the Parser Plugin.
         *
         * <p>It would be executed at the end of {@link #transaction(org.embulk.config.ConfigSource, ParserPlugin.Control)}.
         *
         * @param taskSource  {@link org.embulk.config.TaskSource} processed for tasks from {@link org.embulk.config.ConfigSource}
         * @param schema  {@link org.embulk.spi.Schema} to be parsed to
         *
         * @since 0.4.0
         */
        void run(TaskSource taskSource, Schema schema);
    }

    /**
     * Processes the entire parsing transaction.
     *
     * @param config  a configuration for the Parser Plugin given from a user
     * @param control  a controller of the following tasks provided from the Embulk core
     *
     * @since 0.4.0
     */
    void transaction(ConfigSource config, ParserPlugin.Control control);

    /**
     * Runs each parsing task.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param schema  {@link org.embulk.spi.Schema} to be parsed to
     * @param input  {@link org.embulk.spi.FileOutput} that is read from a File Input Plugin, or a Decoder Plugin
     * @param output  {@link org.embulk.spi.PageOutput} to write parsed input so that the input is read from an Output Plugin, or
     *     another Filter Plugin
     *
     * @since 0.4.0
     */
    void run(TaskSource taskSource, Schema schema, FileInput input, PageOutput output);

    /**
     * Runs each parsing task and return TaskReport
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param schema  {@link org.embulk.spi.Schema} to be parsed to
     * @param input  {@link org.embulk.spi.FileOutput} that is read from a File Input Plugin, or a Decoder Plugin
     * @param output  {@link org.embulk.spi.PageOutput} to write parsed input so that the input is read from an Output Plugin, or
     *     another Filter Plugin
     * @return the {@link TaskReport} in {@link java.util.Optional}
     */
    default Optional<TaskReport> runThenReturnTaskReport(TaskSource taskSource, Schema schema, FileInput input, PageOutput output) {
        this.run(taskSource, schema, input, output);
        return Optional.empty();
    }
}
