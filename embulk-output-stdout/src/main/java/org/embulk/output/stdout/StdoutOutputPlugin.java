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

package org.embulk.output.stdout;

import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.Exec;
import org.embulk.spi.OutputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.TransactionalPageOutput;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdoutOutputPlugin implements OutputPlugin {
    public interface PluginTask extends Task {
        @Config("prints_column_names")
        @ConfigDefault("false")
        public boolean getPrintsColumnNames();

        @Config("timezone")
        @ConfigDefault("\"UTC\"")
        public String getTimeZoneId();
    }

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public ConfigDiff transaction(ConfigSource config,
            Schema schema, int taskCount,
            OutputPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, PluginTask.class);
        if (task.getPrintsColumnNames()) {
            for (final Column column : schema.getColumns()) {
                if (column.getIndex() > 0) {
                    System.out.print(",");
                }
                System.out.print(column.getName());
            }
            System.out.println("");
        }
        return resume(task.dump(), schema, taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            OutputPlugin.Control control) {
        control.run(taskSource);
        return CONFIG_MAPPER_FACTORY.newConfigDiff();
    }

    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<TaskReport> successTaskReports) {}

    @Override
    public TransactionalPageOutput open(TaskSource taskSource, final Schema schema,
            int taskIndex) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);

        return new TransactionalPageOutput() {
            private final PageReader reader = getPageReader(schema);
            private final PagePrinter printer = new PagePrinter(schema, task.getTimeZoneId());

            public void add(Page page) {
                reader.setPage(page);
                while (reader.nextRecord()) {
                    System.out.println(printer.printRecord(reader, ","));
                }
            }

            public void finish() {
                System.out.flush();
            }

            public void close() {
                reader.close();
            }

            public void abort() {}

            public TaskReport commit() {
                return CONFIG_MAPPER_FACTORY.newTaskReport();
            }
        };
    }

    @SuppressWarnings("deprecation")  // For the use of new PageReader().
    private static PageReader getPageReader(final Schema schema) {
        try {
            return Exec.getPageReader(schema);
        } catch (final NoSuchMethodError ex) {
            // Exec.getPageReader() is available from v0.10.17, and "new PageReader()" is deprecated then.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-output-stdout is expected to work with Embulk v0.10.17+.", ex);
            return new PageReader(schema);
        }
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();

    private static final Logger logger = LoggerFactory.getLogger(StdoutOutputPlugin.class);
}
