/*
 * Copyright 2016 The Embulk project
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

package org.embulk.parser.json;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.DataException;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInput;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.Schema;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.type.Types;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.embulk.util.config.units.ColumnConfig;
import org.embulk.util.config.units.SchemaConfig;
import org.embulk.util.file.FileInputInputStream;
import org.embulk.util.json.JsonParseException;
import org.embulk.util.json.JsonParser;
import org.embulk.util.timestamp.TimestampFormatter;
import org.msgpack.core.Preconditions;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonParserPlugin implements ParserPlugin {

    public JsonParserPlugin() {
        this.jsonParser = new JsonParser();
    }

    public enum InvalidEscapeStringPolicy {
        PASSTHROUGH("PASSTHROUGH"),
        SKIP("SKIP"),
        UNESCAPE("UNESCAPE");

        private final String string;

        private InvalidEscapeStringPolicy(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }
    }

    public interface PluginTask extends Task {
        @Config("stop_on_invalid_record")
        @ConfigDefault("false")
        boolean getStopOnInvalidRecord();

        @Config("invalid_string_escapes")
        @ConfigDefault("\"PASSTHROUGH\"")
        InvalidEscapeStringPolicy getInvalidEscapeStringPolicy();

        @Config("root")
        @ConfigDefault("null")
        Optional<String> getRoot();

        @Config("flatten_json_array")
        @ConfigDefault("false")
        boolean getFlattenJsonArray();

        @Config("columns")
        @ConfigDefault("null")
        Optional<SchemaConfig> getSchemaConfig();

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

    public interface OptionalColumnConfig extends Task {
        @Config("element_at")
        @ConfigDefault("null")
        Optional<String> getElementAt();

        // From org.embulk.spi.time.TimestampParser.TimestampColumnOption.
        @Config("timezone")
        @ConfigDefault("null")
        Optional<String> getTimeZoneId();

        // From org.embulk.spi.time.TimestampParser.TimestampColumnOption.
        @Config("format")
        @ConfigDefault("null")
        Optional<String> getFormat();

        // From org.embulk.spi.time.TimestampParser.TimestampColumnOption.
        @Config("date")
        @ConfigDefault("null")
        Optional<String> getDate();
    }

    @Override
    @SuppressWarnings("deprecation")  // For the use of task#dump().
    public void transaction(ConfigSource configSource, Control control) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createConfigMapper().map(configSource, PluginTask.class);
        control.run(task.dump(), newSchema(task));
    }

    Schema newSchema(PluginTask task) {
        if (isUsingCustomSchema(task)) {
            return task.getSchemaConfig().get().toSchema();
        } else {
            return Schema.builder().add("record", Types.JSON).build(); // generate a schema
        }
    }

    @Override
    public void run(TaskSource taskSource, Schema schema, FileInput input, PageOutput output) {
        final PluginTask task = CONFIG_MAPPER_FACTORY.createTaskMapper().map(taskSource, PluginTask.class);

        final boolean stopOnInvalidRecord = task.getStopOnInvalidRecord();
        final Map<Column, TimestampFormatter> timestampFormatters = new HashMap<>();
        final Map<Column, String> jsonPointers = new HashMap<>();
        if (isUsingCustomSchema(task)) {
            final SchemaConfig schemaConfig = task.getSchemaConfig().get();
            timestampFormatters.putAll(newTimestampColumnFormattersAsMap(task, task.getSchemaConfig().get()));
            jsonPointers.putAll(createJsonPointerMap(schema, schemaConfig));
        }

        try (final PageBuilder pageBuilder = getPageBuilder(Exec.getBufferAllocator(), schema, output);
                FileInputInputStream in = new FileInputInputStream(input)) {
            while (in.nextFile()) {
                final String fileName = input.hintOfCurrentInputFileNameForLogging().orElse("-");

                boolean evenOneJsonParsed = false;
                try (JsonParser.Stream stream = newJsonStream(in, task)) {
                    Value originalValue;
                    while ((originalValue = stream.next()) != null) {
                        try {
                            Value value = originalValue;
                            if (task.getRoot().isPresent()) {
                                try {
                                    value = jsonParser.parseWithOffsetInJsonPointer(originalValue.toJson(), task.getRoot().get());
                                } catch (JsonParseException e) {
                                    throw new JsonRecordValidateException("A Json record doesn't have given 'JSON pointer to root'.");
                                }
                            }

                            final Iterable<Value> recordValues;
                            if (task.getFlattenJsonArray()) {
                                if (!value.isArrayValue()) {
                                    throw new JsonRecordValidateException(
                                            String.format(
                                                    "A Json record must represent array value with '__experimental__flatten_json_array' option, but it's %s",
                                                    value.getValueType().name()));
                                }
                                recordValues = value.asArrayValue();
                            } else {
                                recordValues = Collections.singletonList(value);
                            }

                            for (Value recordValue : recordValues) {
                                addRecord(task, pageBuilder, schema, timestampFormatters, jsonPointers, recordValue);
                                evenOneJsonParsed = true;
                            }
                        } catch (JsonRecordValidateException e) {
                            if (stopOnInvalidRecord) {
                                throw new DataException(String.format("Invalid record in %s: %s", fileName, originalValue.toJson()), e);
                            }
                            logger.warn(String.format("Skipped record in %s (%s): %s", fileName, e.getMessage(), originalValue.toJson()));
                        }
                    }
                } catch (IOException | JsonParseException e) {
                    if (Exec.isPreview() && evenOneJsonParsed) {
                        // JsonParseException occurs when it cannot parse the last part of sampling buffer. Because
                        // the last part is sometimes invalid as JSON data. Therefore JsonParseException can be
                        // ignore in preview if at least one JSON is already parsed.
                        break;
                    }
                    throw new DataException(String.format("Failed to parse JSON: %s", fileName), e);
                }
            }

            pageBuilder.finish();
        }
    }

    private void addRecord(
            PluginTask task,
            PageBuilder pageBuilder,
            Schema schema,
            Map<Column, TimestampFormatter> timestampFormatters,
            Map<Column, String> jsonPointers,
            Value value) {
        if (!value.isMapValue()) {
            throw new JsonRecordValidateException(
                    String.format("A Json record must represent map value but it's %s", value.getValueType().name()));
        }

        try {
            if (isUsingCustomSchema(task)) {
                setValueWithCustomSchema(pageBuilder, schema, timestampFormatters, jsonPointers, value.asMapValue());
            } else {
                setValueWithSingleJsonColumn(pageBuilder, schema, value.asMapValue());
            }
        } catch (final DateTimeParseException ex) {
            throw new JsonRecordValidateException(
                    String.format("A Json record must have valid timestamp value but it's %s", value.getValueType().name()));
        }
        pageBuilder.addRecord();
    }

    private static boolean isUsingCustomSchema(PluginTask task) {
        return task.getSchemaConfig().isPresent() && !task.getSchemaConfig().get().isEmpty();
    }

    private static void setValueWithSingleJsonColumn(PageBuilder pageBuilder, Schema schema, MapValue value) {
        final Column column = schema.getColumn(0); // record column
        pageBuilder.setJson(column, value);
    }

    private void setValueWithCustomSchema(
            PageBuilder pageBuilder,
            Schema schema,
            Map<Column, TimestampFormatter> timestampFormatters,
            Map<Column, String> jsonPointers,
            MapValue value) {
        final Map<Value, Value> map = value.map();
        String valueAsJsonString = null;
        if (!jsonPointers.isEmpty()) {
            // Convert to string in order to re-parse with given JSON pointer
            valueAsJsonString = value.toJson();
        }
        for (Column column : schema.getColumns()) {
            final String jsonPointer = jsonPointers.get(column);
            final Value columnValue;
            if (jsonPointer != null) {
                columnValue = parseColumnValueWithOffsetInJsonPointer(valueAsJsonString, jsonPointer);
            } else {
                columnValue = map.get(ValueFactory.newString(column.getName()));
            }

            if (columnValue == null || columnValue.isNilValue()) {
                pageBuilder.setNull(column);
                continue;
            }

            column.visit(new ColumnVisitor() {
                @Override
                public void booleanColumn(Column column) {
                    final boolean booleanValue;
                    if (columnValue.isBooleanValue()) {
                        booleanValue = columnValue.asBooleanValue().getBoolean();
                    } else {
                        booleanValue = Boolean.parseBoolean(columnValue.toString());
                    }
                    pageBuilder.setBoolean(column, booleanValue);
                }

                @Override
                public void longColumn(Column column) {
                    final long longValue;
                    if (columnValue.isIntegerValue()) {
                        longValue = columnValue.asIntegerValue().toLong();
                    } else {
                        longValue = Long.parseLong(columnValue.toString());
                    }
                    pageBuilder.setLong(column, longValue);
                }

                @Override
                public void doubleColumn(Column column) {
                    final double doubleValue;
                    if (columnValue.isFloatValue()) {
                        doubleValue = columnValue.asFloatValue().toDouble();
                    } else {
                        doubleValue = Double.parseDouble(columnValue.toString());
                    }
                    pageBuilder.setDouble(column, doubleValue);
                }

                @Override
                public void stringColumn(Column column) {
                    pageBuilder.setString(column, columnValue.toString());
                }

                @Override
                public void timestampColumn(Column column) {
                    final Instant timestampInstant = timestampFormatters.get(column).parse(columnValue.toString());
                    setParsedTimestamp(pageBuilder, column, timestampInstant);
                }

                @Override
                public void jsonColumn(Column column) {
                    pageBuilder.setJson(column, columnValue);
                }
            });
        }
    }

    @SuppressWarnings("deprecation")  // For the use of new PageBuilder().
    private static PageBuilder getPageBuilder(final BufferAllocator allocator, final Schema schema, final PageOutput output) {
        try {
            return Exec.getPageBuilder(allocator, schema, output);
        } catch (final NoSuchMethodError ex) {
            // Exec.getPageBuilder() is available from v0.10.17, and "new PageBuidler()" is deprecated then.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-parser-json is expected to work with Embulk v0.10.17+.", ex);
            return new PageBuilder(allocator, schema, output);
        }
    }

    private Value parseColumnValueWithOffsetInJsonPointer(String valueAsJsonString, String jsonPointer) {
        try {
            return jsonParser.parseWithOffsetInJsonPointer(valueAsJsonString, jsonPointer);
        } catch (JsonParseException e) {
            /*
             * When JsonParseException is thrown, it would be an error that the given JSON pointer doesn't match with the JSON object.
             * We would return NULL when the pointer doesn't match, not throw Exception.
             *
             * NOTE: We may change the behavior (ref: https://github.com/embulk/embulk/pull/1103#discussion_r255807991)
             */
            return ValueFactory.newNil();
        }
    }

    private JsonParser.Stream newJsonStream(FileInputInputStream in, PluginTask task)
            throws IOException {
        final InvalidEscapeStringPolicy policy = task.getInvalidEscapeStringPolicy();
        final InputStream inputStream;
        switch (policy) {
            case SKIP:
            case UNESCAPE:
                byte[] lines = new BufferedReader(new InputStreamReader(in))
                        .lines()
                        .map(invalidEscapeStringFunction(policy))
                        .collect(Collectors.joining())
                        .getBytes(StandardCharsets.UTF_8);
                inputStream = new ByteArrayInputStream(lines);
                break;
            case PASSTHROUGH:
            default:
                inputStream = in;
        }

        return jsonParser.open(inputStream);
    }

    static Function<String, String> invalidEscapeStringFunction(final InvalidEscapeStringPolicy policy) {
        return input -> {
            Preconditions.checkNotNull(input);
            if (policy == InvalidEscapeStringPolicy.PASSTHROUGH) {
                return input;
            }
            StringBuilder builder = new StringBuilder();
            char[] charArray = input.toCharArray();
            for (int characterIndex = 0; characterIndex < charArray.length; characterIndex++) {
                char c = charArray[characterIndex];
                if (c == '\\') {
                    if (charArray.length > characterIndex + 1) {
                        char next = charArray[characterIndex + 1];
                        switch (next) {
                            case 'b':
                            case 'f':
                            case 'n':
                            case 'r':
                            case 't':
                            case '"':
                            case '\\':
                            case '/':
                                builder.append(c);
                                break;
                            case 'u': // hexstring such as \u0001
                                if (charArray.length > characterIndex + 5) {
                                    char[] hexChars = {charArray[characterIndex + 2], charArray[characterIndex + 3], charArray[characterIndex + 4],
                                                       charArray[characterIndex + 5]};
                                    String hexString = new String(hexChars);
                                    if (DIGITS_PATTERN.matcher(hexString).matches()) {
                                        builder.append(c);
                                    } else {
                                        if (policy == InvalidEscapeStringPolicy.SKIP) {
                                            // remove \\u
                                            characterIndex++;
                                        }
                                    }
                                }
                                break;
                            default:
                                switch (policy) {
                                    case SKIP:
                                        characterIndex++;
                                        break;
                                    case UNESCAPE:
                                        break;
                                    default:  // Do nothing, and just pass through.
                                }
                                break;
                        }
                    }
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        };
    }

    private static Map<Column, String> createJsonPointerMap(Schema schema, SchemaConfig config) {
        Map<Column, String> result = new HashMap<>();
        final List<Column> columns = schema.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            final Column column = columns.get(i);
            final ColumnConfig columnConfig = config.getColumn(i);
            final OptionalColumnConfig options =
                    CONFIG_MAPPER_FACTORY.createConfigMapper().map(columnConfig.getOption(), OptionalColumnConfig.class);
            if (options.getElementAt().isPresent()) {
                result.put(column, options.getElementAt().get());
            }
        }
        return result;
    }

    private static Map<Column, TimestampFormatter> newTimestampColumnFormattersAsMap(
            final PluginTask task,
            final SchemaConfig schema) {
        final Map<Column, TimestampFormatter> formatters = new HashMap<>();
        int i = 0;
        for (final ColumnConfig column : schema.getColumns()) {
            if (column.getType() instanceof TimestampType) {
                final OptionalColumnConfig option =
                        CONFIG_MAPPER_FACTORY.createConfigMapper().map(column.getOption(), OptionalColumnConfig.class);
                final String pattern = option.getFormat().orElse(task.getDefaultTimestampFormat());
                final TimestampFormatter formatter = TimestampFormatter.builder(pattern, true)
                        .setDefaultZoneFromString(option.getTimeZoneId().orElse(task.getDefaultTimeZoneId()))
                        .setDefaultDateFromString(option.getDate().orElse(task.getDefaultDate()))
                        .build();
                formatters.put(column.toColumn(i), formatter);
            }
            i++;
        }
        return Collections.unmodifiableMap(formatters);
    }

    @SuppressWarnings("deprecation")  // For the use of new PageBuilder with java.time.Instant.
    private static void setParsedTimestamp(final PageBuilder pageBuilder, final Column column, final Instant instant) {
        try {
            pageBuilder.setTimestamp(column, instant);
        } catch (final NoSuchMethodError ex) {
            // PageBuilder with Instant are available from v0.10.13, and org.embulk.spi.Timestamp is deprecated.
            // It is not expected to happen because this plugin is embedded with Embulk v0.10.24+, but falling back just in case.
            // TODO: Remove this fallback in v0.11.
            logger.warn("embulk-parser-json is expected to work with Embulk v0.10.17+.", ex);
            pageBuilder.setTimestamp(column, org.embulk.spi.time.Timestamp.ofInstant(instant));
        }
    }

    static class JsonRecordValidateException extends DataException {
        JsonRecordValidateException(String message) {
            super(message);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(JsonParserPlugin.class);

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder().addDefaultModules().build();

    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\p{XDigit}+");

    private final JsonParser jsonParser;
}
