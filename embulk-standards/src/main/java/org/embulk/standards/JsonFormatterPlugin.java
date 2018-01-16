package org.embulk.standards;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.DataException;
import org.embulk.spi.FileOutput;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampFormatter;
import org.embulk.spi.util.LineEncoder;
import org.embulk.spi.util.Timestamps;
import org.msgpack.value.Value;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonFormatterPlugin
        implements FormatterPlugin
{
    public interface PluginTask
            extends Task, LineEncoder.EncoderTask, TimestampFormatter.Task
    {
        @Config("column_options")
        @ConfigDefault("{}")
        Map<String, TimestampColumnOption> getColumnOptions();
    }

    public interface TimestampColumnOption
            extends Task, TimestampFormatter.TimestampColumnOption
    { }

    @Override
    public void transaction(ConfigSource configSource, Schema schema, Control control)
    {
        PluginTask task = configSource.loadConfig(PluginTask.class);
        // validate column_options
        for (String columnName : task.getColumnOptions().keySet()) {
            schema.lookupColumn(columnName);  // throws SchemaConfigException
        }
        control.run(task.dump());
    }

    @Override
    public PageOutput open(TaskSource taskSource, final Schema schema,
                           FileOutput output)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);
        final LineEncoder encoder = new LineEncoder(output, task);
        final TimestampFormatter[] timestampFormatters = Timestamps.newTimestampColumnFormatters(task, schema, task.getColumnOptions());

        // create a file
        encoder.nextFile();

        return new PageOutput() {
            private final PageReader pageReader = new PageReader(schema);
            public void add(Page page)
            {
                pageReader.setPage(page);
                final ObjectMapper objectMapper = new ObjectMapper();
                while (pageReader.nextRecord()) {
                    final LinkedHashMap<String, Object> line = new LinkedHashMap<>();
                    schema.visitColumns(new ColumnVisitor() {
                        public void booleanColumn(Column column)
                        {
                            if (!pageReader.isNull(column)) {
                                addValue(column.getName(), pageReader.getBoolean(column));
                            } else {
                                addNull(column.getName());
                            }
                        }

                        public void longColumn(Column column)
                        {
                            if (!pageReader.isNull(column)) {
                                addValue(column.getName(), pageReader.getLong(column));
                            } else {
                                addNull(column.getName());
                            }
                        }

                        public void doubleColumn(Column column)
                        {
                            if (!pageReader.isNull(column)) {
                                addValue(column.getName(), pageReader.getDouble(column));
                            } else {
                                addNull(column.getName());
                            }
                        }

                        public void stringColumn(Column column)
                        {
                            if (!pageReader.isNull(column)) {
                                addValue(column.getName(), pageReader.getString(column));
                            } else {
                                addNull(column.getName());
                            }
                        }

                        public void timestampColumn(Column column)
                        {
                            if (!pageReader.isNull(column)) {
                                Timestamp value = pageReader.getTimestamp(column);
                                addValue(column.getName(), timestampFormatters[column.getIndex()].format(value));
                            } else {
                                addNull(column.getName());
                            }
                        }

                        public void jsonColumn(Column column)
                        {
                            if (!pageReader.isNull(column)) {
                                Value value = pageReader.getJson(column);
                                addValue(column.getName(), value.toJson());
                            } else {
                                addNull(column.getName());
                            }
                        }

                        private void addValue(String name, Object value)
                        {
                            line.put(name, value);
                        }

                        private void addNull(String name)
                        {
                            line.put(name, null);
                        }
                    });
                    try {
                        encoder.addText(objectMapper.writeValueAsString(line));
                    } catch (JsonProcessingException e) {
                        throw new JsonEncodeFailedException(e.getMessage());
                    }
                    encoder.addNewLine();
                }
            }

            public void finish()
            {
                encoder.finish();
            }

            public void close()
            {
                encoder.close();
            }
        };
    }

    static class JsonEncodeFailedException
            extends DataException
    {
        JsonEncodeFailedException(String message)
        {
            super(message);
        }
    }
}
