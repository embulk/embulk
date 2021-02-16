/*
 * Copyright 2017 The Embulk project
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

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.DataException;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.type.TimestampType;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.embulk.util.config.units.ColumnConfig;
import org.embulk.util.config.units.SchemaConfig;
import org.embulk.util.json.JsonParseException;
import org.embulk.util.json.JsonParser;
import org.embulk.util.timestamp.TimestampFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigInputPlugin implements InputPlugin {
    private interface PluginTask extends Task {
        @Config("columns")
        SchemaConfig getSchemaConfig();

        @Config("values")
        List<List<List<JsonNode>>> getValues();

        // From org.embulk.spi.time.TimestampParser.Task.
        @Config("default_timezone")
        @ConfigDefault("\"UTC\"")
        String getDefaultTimeZoneId();

        // From org.embulk.spi.time.TimestampParser.Task.
        @Config("default_timestamp_format")
        @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%N %z\"")
        String getDefaultTimestampFormat();

        // From org.embulk.spi.time.TimestampParser.Task.
        @Config("default_date")
        @ConfigDefault("\"1970-01-01\"")
        String getDefaultDate();
    }

    // From org.embulk.spi.time.TimestampParser.TimestampColumnOption.
    private interface TimestampColumnOption extends Task {
        @Config("timezone")
        @ConfigDefault("null")
        Optional<String> getTimeZoneId();

        @Config("format")
        @ConfigDefault("null")
        Optional<String> getFormat();

        @Config("date")
        @ConfigDefault("null")
        Optional<String> getDate();
    }

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public ConfigDiff transaction(ConfigSource config,
            InputPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(config, PluginTask.class);
        final Schema schema = task.getSchemaConfig().toSchema();
        final List<List<List<JsonNode>>> values = task.getValues();
        final int taskCount = values.size();

        return resume(task.dump(), schema, taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            InputPlugin.Control control) {
        control.run(taskSource, schema, taskCount);
        return CONFIG_MAPPER_FACTORY.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<TaskReport> successTaskReports) {}

    @Override
    public TaskReport run(TaskSource taskSource,
            Schema schema, int taskIndex,
            PageOutput output) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);
        final List<List<JsonNode>> taskValues = task.getValues().get(taskIndex);
        final TimestampFormatter[] timestampFormatters = newTimestampColumnFormatters(task, task.getSchemaConfig());
        final JsonParser jsonParser = new JsonParser();

        try (final PageBuilder pageBuilder = getPageBuilder(Exec.getBufferAllocator(), schema, output)) {
            for (final List<JsonNode> rowValues : taskValues) {
                schema.visitColumns(new ColumnVisitor() {
                        public void booleanColumn(Column column) {
                            final JsonNode value = rowValues.get(column.getIndex());
                            if (value == null || value.isNull()) {
                                pageBuilder.setNull(column);
                            } else {
                                pageBuilder.setBoolean(column, value.asBoolean());
                            }
                        }

                        public void longColumn(Column column) {
                            final JsonNode value = rowValues.get(column.getIndex());
                            if (value == null || value.isNull()) {
                                pageBuilder.setNull(column);
                            } else {
                                pageBuilder.setLong(column, value.asLong());
                            }
                        }

                        public void doubleColumn(Column column) {
                            final JsonNode value = rowValues.get(column.getIndex());
                            if (value == null || value.isNull()) {
                                pageBuilder.setNull(column);
                            } else {
                                pageBuilder.setDouble(column, value.asDouble());
                            }
                        }

                        public void stringColumn(Column column) {
                            final JsonNode value = rowValues.get(column.getIndex());
                            if (value == null || value.isNull()) {
                                pageBuilder.setNull(column);
                            } else {
                                pageBuilder.setString(column, value.asText());
                            }
                        }

                        public void timestampColumn(Column column) {
                            final JsonNode value = rowValues.get(column.getIndex());
                            if (value == null || value.isNull()) {
                                pageBuilder.setNull(column);
                            } else {
                                final Instant instant;
                                try {
                                    instant = timestampFormatters[column.getIndex()].parse(value.asText());
                                } catch (final DateTimeParseException ex) {
                                    throw new DataException(ex);
                                }

                                setParsedTimestamp(pageBuilder, column, instant);
                            }
                        }

                        public void jsonColumn(Column column) {
                            final JsonNode value = rowValues.get(column.getIndex());
                            if (value == null || value.isNull()) {
                                pageBuilder.setNull(column);
                            } else {
                                try {
                                    pageBuilder.setJson(column, jsonParser.parse(value.toString()));
                                } catch (JsonParseException ex) {
                                    throw new DataException(ex);
                                }
                            }
                        }
                    });
                pageBuilder.addRecord();
            }
            pageBuilder.finish();
        }

        return CONFIG_MAPPER_FACTORY.newTaskReport();
    }

    @Override
    public ConfigDiff guess(ConfigSource config) {
        return CONFIG_MAPPER_FACTORY.newConfigDiff();
    }

    @SuppressWarnings("deprecation")  // For the use of new PageBuilder().
    private static PageBuilder getPageBuilder(final BufferAllocator allocator, final Schema schema, final PageOutput output) {
        try {
            return Exec.getPageBuilder(allocator, schema, output);
        } catch (final NoSuchMethodError ex) {
            // Exec.getPageBuilder() is available from v0.10.17, and "new PageBuidler()" is deprecated then.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-filter-remove_columns is expected to work with Embulk v0.10.17+.", ex);
            return new PageBuilder(allocator, schema, output);
        }
    }

    @SuppressWarnings("deprecation")  // https://github.com/embulk/embulk/issues/1289
    private static TimestampFormatter[] newTimestampColumnFormatters(
            final PluginTask task,
            final SchemaConfig schema) {
        final TimestampFormatter[] formatters = new TimestampFormatter[schema.getColumnCount()];
        int i = 0;
        for (final ColumnConfig column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                final TimestampColumnOption columnOption =
                        CONFIG_MAPPER_FACTORY.createConfigMapper().map(column.getOption(), TimestampColumnOption.class);

                final String pattern = columnOption.getFormat().orElse(task.getDefaultTimestampFormat());
                formatters[i] = TimestampFormatter.builder(pattern, true)
                        .setDefaultZoneFromString(columnOption.getTimeZoneId().orElse(task.getDefaultTimeZoneId()))
                        .setDefaultDateFromString(columnOption.getDate().orElse(task.getDefaultDate()))
                        .build();
            }
            i++;
        }
        return formatters;
    }

    @SuppressWarnings("deprecation")  // For the use of new PageBuilder with java.time.Instant.
    private static void setParsedTimestamp(final PageBuilder pageBuilder, final Column column, final Instant instant) {
        try {
            pageBuilder.setTimestamp(column, instant);
        } catch (final NoSuchMethodError ex) {
            // PageBuilder with Instant are available from v0.10.13, and org.embulk.spi.Timestamp is deprecated.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-input-config is expected to work with Embulk v0.10.17+.", ex);
            pageBuilder.setTimestamp(column, org.embulk.spi.time.Timestamp.ofInstant(instant));
        }
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();

    private static final Logger logger = LoggerFactory.getLogger(ConfigInputPlugin.class);
}
