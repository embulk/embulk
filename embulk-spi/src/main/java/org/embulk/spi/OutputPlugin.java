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
 * The main class that an Output Plugin implements.
 *
 * <p>An Output Plugin writes a sequence of data records in {@link org.embulk.spi.Page}s read from an Input Plugin, or a Filter
 * Plugin, into the configured destination.
 *
 * @since 0.4.0
 */
public interface OutputPlugin {
    /**
     * A controller of the following tasks provided from the Embulk core.
     *
     * @since 0.4.0
     */
    interface Control {
        /**
         * Runs the following tasks of the Output Plugin.
         *
         * <p>It would be executed at the end of
         * {@link #transaction(org.embulk.config.ConfigSource, org.embulk.spi.Schema, int, OutputPlugin.Control)}.
         *
         * @param taskSource  {@link org.embulk.config.TaskSource} processed for tasks from {@link org.embulk.config.ConfigSource}
         * @return reports of tasks
         *
         * @since 0.7.0
         */
        List<TaskReport> run(TaskSource taskSource);
    }

    /**
     * Processes the entire output transaction.
     *
     * @param config  a configuration for the Output Plugin given from a user
     * @param schema  {@link org.embulk.spi.Schema} of the output
     * @param taskCount  the number of tasks
     * @param control  a controller of the following tasks provided from the Embulk core
     * @return {@link org.embulk.config.ConfigDiff} to represent the difference the next incremental run
     *
     * @since 0.4.0
     */
    ConfigDiff transaction(ConfigSource config, Schema schema, int taskCount, OutputPlugin.Control control);

    /**
     * Resumes an output transaction.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param schema  {@link org.embulk.spi.Schema} of the output
     * @param taskCount  the number of tasks
     * @param control  a controller of the following tasks provided from the Embulk core
     * @return {@link org.embulk.config.ConfigDiff} to represent the difference the next incremental run
     *
     * @since 0.4.0
     */
    ConfigDiff resume(TaskSource taskSource, Schema schema, int taskCount, OutputPlugin.Control control);

    /**
     * Cleans up resources used in the transaction.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param schema  {@link org.embulk.spi.Schema} of the output
     * @param taskCount  the number of tasks
     * @param successTaskReports  reports of successful tasks
     *
     * @since 0.7.0
     */
    void cleanup(TaskSource taskSource, Schema schema, int taskCount, List<TaskReport> successTaskReports);

    /**
     * Opens a {@link org.embulk.spi.TransactionalPageOutput} instance that receives {@link org.embulk.spi.Page}s from an Input
     * Plugin, or a Filter Plugin, and writes them into the configured destination.
     *
     * <p>It processes each output task.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param schema  {@link org.embulk.spi.Schema} of the output
     * @param taskIndex  the index number of the task
     * @return an implementation of {@link org.embulk.spi.TransactionalPageOutput} that receives {@link org.embulk.spi.Page}s
     *     from an Input Plugin, or a Filter Plugin, and writes them into the configured destination
     *
     * @since 0.4.0
     */
    TransactionalPageOutput open(TaskSource taskSource, Schema schema, int taskIndex);
}
