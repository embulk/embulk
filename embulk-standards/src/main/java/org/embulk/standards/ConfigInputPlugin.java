package org.embulk.standards;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.embulk.config.Config;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.DataException;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.json.JsonParseException;
import org.embulk.spi.json.JsonParser;
import org.embulk.spi.time.TimestampParseException;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.util.Timestamps;

public class ConfigInputPlugin implements InputPlugin {
    private interface PluginTask extends Task, TimestampParser.Task {
        @Config("columns")
        SchemaConfig getSchemaConfig();

        @Config("values")
        List<List<List<JsonNode>>> getValues();
    }

    @Override
    public ConfigDiff transaction(ConfigSource config,
            InputPlugin.Control control) {
        final PluginTask task = config.loadConfig(PluginTask.class);
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
        return Exec.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<TaskReport> successTaskReports) {}

    @Override
    public TaskReport run(TaskSource taskSource,
            Schema schema, int taskIndex,
            PageOutput output) {
        final PluginTask task = taskSource.loadTask(PluginTask.class);
        final List<List<JsonNode>> taskValues = task.getValues().get(taskIndex);
        final TimestampParser[] timestampParsers = Timestamps.newTimestampColumnParsers(task, task.getSchemaConfig());
        final JsonParser jsonParser = new JsonParser();

        try (final PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), schema, output)) {
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
                                try {
                                    pageBuilder.setTimestamp(column,
                                                             timestampParsers[column.getIndex()].parse(value.asText()));
                                } catch (TimestampParseException ex) {
                                    throw new DataException(ex);
                                }
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

        return Exec.newTaskReport();
    }

    @Override
    public ConfigDiff guess(ConfigSource config) {
        return Exec.newConfigDiff();
    }
}
