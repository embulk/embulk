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

import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;

/**
 * The main class that an Input Plugin implements.
 *
 * <p>An Input Plugin reads a sequence of data records from the configured source into {@link org.embulk.spi.PageOutput} so that
 * the read input is read from an Output Plugin, or a Filter Plugin.
 *
 * @since 0.4.0
 */
public interface InputPlugin {
    /**
     * A controller of the following tasks provided from the Embulk core.
     *
     * @since 0.4.0
     */
    interface Control {
        /**
         * Runs the following tasks of the Input Plugin.
         *
         * <p>It would be executed at the end of {@link #transaction(org.embulk.config.ConfigSource, InputPlugin.Control)}.
         *
         * @param taskSource  {@link org.embulk.config.TaskSource} processed for tasks from {@link org.embulk.config.ConfigSource}
         * @param schema  {@link org.embulk.spi.Schema} of the input
         * @param taskCount  the number of tasks
         * @return reports of tasks
         *
         * @since 0.7.0
         */
        List<TaskReport> run(TaskSource taskSource, Schema schema, int taskCount);
    }

    /**
     * Processes the entire input transaction.
     *
     * @param config  a configuration for the Input Plugin given from a user
     * @param control  a controller of the following tasks provided from the Embulk core
     * @return {@link org.embulk.config.ConfigDiff} to represent the difference the next incremental run
     *
     * @since 0.4.0
     */
    ConfigDiff transaction(ConfigSource config, InputPlugin.Control control);

    /**
     * Resumes an input transaction.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param schema  {@link org.embulk.spi.Schema} of the input
     * @param taskCount  the number of tasks
     * @param control  a controller of the following tasks provided from the Embulk core
     * @return {@link org.embulk.config.ConfigDiff} to represent the difference the next incremental run
     *
     * @since 0.4.0
     */
    ConfigDiff resume(TaskSource taskSource, Schema schema, int taskCount, InputPlugin.Control control);

    /**
     * Cleans up resources used in the transaction.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param schema  {@link org.embulk.spi.Schema} of the input
     * @param taskCount  the number of tasks
     * @param successTaskReports  reports of successful tasks
     *
     * @since 0.7.0
     */
    void cleanup(TaskSource taskSource, Schema schema, int taskCount, List<TaskReport> successTaskReports);

    /**
     * Runs each input task.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param schema  {@link org.embulk.spi.Schema} of the input
     * @param taskIndex  the index number of the task
     * @param output  {@link org.embulk.spi.PageOutput} to write read input so that the input is read from an Output Plugin,
     *     or a Filter Plugin
     * @return a report from the task
     *
     * @since 0.7.0
     */
    TaskReport run(TaskSource taskSource, Schema schema, int taskIndex, PageOutput output);

    /**
     * Performs the guess for the partially configured input.
     *
     * @param config  a partial configuration for the Input Plugin given from a user
     * @return a new configuration guessed based on {@code config}
     *
     * @since 0.4.0
     */
    ConfigDiff guess(ConfigSource config);
}
