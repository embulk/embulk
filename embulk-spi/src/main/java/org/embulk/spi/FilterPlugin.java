/*
 * Copyright 2015 The Embulk project
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
 * The main class that a Filter Plugin implements.
 *
 * <p>A Filter Plugin converts a schema and a sequence of data records in {@link org.embulk.spi.PageOutput} from an Input Plugin,
 * or another Filter Plugin, into another schema and another sequence of data records in {@link org.embulk.spi.PageOutput} for an
 * Output Plugin, or another Filter Plugin.
 *
 * @since 0.4.0
 */
public interface FilterPlugin {
    /**
     * A controller of the following tasks provided from the Embulk core.
     *
     * @since 0.4.0
     */
    interface Control {
        /**
         * Runs the following tasks of the Filter Plugin.
         *
         * <p>It would be executed at the end of
         * {@link #transaction(org.embulk.config.ConfigSource, org.embulk.spi.Schema, FilterPlugin.Control)}.
         *
         * @param taskSource  {@link org.embulk.config.TaskSource} processed for tasks from {@link org.embulk.config.ConfigSource}
         * @param outputSchema  {@link org.embulk.spi.Schema} of the output from the filter
         *
         * @since 0.4.0
         */
        void run(TaskSource taskSource, Schema outputSchema);
    }

    /**
     * Processes the entire filtering transaction.
     *
     * @param config  a configuration for the Filter Plugin given from a user
     * @param inputSchema  {@link org.embulk.spi.Schema} of the input for the filter
     * @param control  a controller of the following tasks provided from the Embulk core
     *
     * @since 0.4.0
     */
    void transaction(ConfigSource config, Schema inputSchema, FilterPlugin.Control control);

    /**
     * Opens a {@link org.embulk.spi.PageOutput} instance that receives {@link org.embulk.spi.Page}s from an Input Plugin, or
     * another Filter Plugin, and writes converted output into {@link org.embulk.spi.PageOutput} {@code output} in the parameter
     * list for an Output Plugin, or another Filter Plugin.
     *
     * @param taskSource  a configuration processed for the task from {@link org.embulk.config.ConfigSource}
     * @param inputSchema  {@link org.embulk.spi.Schema} of the input for the filter
     * @param outputSchema  {@link org.embulk.spi.Schema} of the output from the filter
     * @param output  {@link org.embulk.spi.PageOutput} to write converted input for an Output Plugin, or another Filter Plugin
     * @return an implementation of {@link org.embulk.spi.PageOutput} that receives {@link org.embulk.spi.Page}s from an Input
     *     Plugin, or another Filter Plugin, and writes converted output into {@link org.embulk.spi.PageOutput} {@code output}
     *     in the parameter list for an Output Plugin, or another Filter Plugin
     *
     * @since 0.4.0
     */
    PageOutput open(TaskSource taskSource, Schema inputSchema, Schema outputSchema, PageOutput output);
}
