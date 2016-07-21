package org.embulk.standards;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.DataException;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInput;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.Schema;
import org.embulk.spi.json.JsonParseException;
import org.embulk.spi.json.JsonUtil;
import org.embulk.spi.type.Types;
import org.embulk.spi.util.FileInputInputStream;
import org.embulk.spi.util.LineDecoder;
import org.msgpack.value.Value;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class JsonParserPlugin
        implements ParserPlugin
{
    public interface PluginTask
            extends Task, LineDecoder.DecoderTask
    {
        @Config("file_type")
        @ConfigDefault("\"sequence\"")
        FileType getFileType();

        @Config("object_field")
        @ConfigDefault("null")
        Optional<String> getObjectField();

        @Config("stop_on_invalid_record")
        @ConfigDefault("false")
        boolean getStopOnInvalidRecord();
    }

    public enum FileType
    {
        SEQUENCE("sequence"), OBJECT("object"), ARRAY("array"), LINES("lines");

        private final String name;

        FileType(String name)
        {
            this.name = name;
        }

        @JsonValue
        @Override
        public String toString()
        {
            return name;
        }

        @JsonCreator
        public static FileType of(String name)
        {
            for (FileType type : FileType.values()) {
                if (type.toString().equals(name)) {
                    return type;
                }
            }
            throw new ConfigException(String.format("Unknown FileType '%s'. Available options are [sequence, object, array, lines]", name));
        }
    }

    private final Logger log;
    private final JsonFactory factory;

    public JsonParserPlugin()
    {
        this.log = Exec.getLogger(JsonParserPlugin.class);
        this.factory = new JsonFactory().enable(Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
    }

    @Override
    public void transaction(ConfigSource configSource, Control control)
    {
        PluginTask task = configSource.loadConfig(PluginTask.class);

        switch (task.getFileType()) {
        case OBJECT:
            if (!task.getObjectField().isPresent()) {
                throw new ConfigException("Must specify 'object_field' option if 'object' is used as file_type option");
            }
            break;
        default:
            break;
        }

        control.run(task.dump(), newSchema());
    }

    @VisibleForTesting
    Schema newSchema()
    {
        return Schema.builder().add("record", Types.JSON).build(); // generate a schema
    }

    @Override
    public void run(TaskSource taskSource, Schema schema, FileInput input, PageOutput output)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);

        final FileType fileType = task.getFileType();
        switch (fileType) {
        case LINES:
            doRunWithLineDecoder(task, schema, input, output);
            break;
        default:
            doRun(task, schema, input, output);
            break;
        }
    }

    void doRunWithLineDecoder(PluginTask task, Schema schema, FileInput input, PageOutput output)
    {
        final boolean stopOnInvalidRecord = task.getStopOnInvalidRecord();
        final Column column = schema.getColumn(0); // record column

        try (PageBuilder pageBuilder = newPageBuilder(schema, output)) {
            while (input.nextFile()) {
                try (LineDecoder lines = new LineDecoder(input, task)) {
                    int lineNumber = 0;
                    String line;
                    Value value;
                    while (true) {
                        if ((line = lines.poll()) == null) { // RuntimeException
                            break;
                        }
                        lineNumber++;

                        try (JsonParser jp = newJsonParser(line)) {
                            value = validateValueType(JsonUtil.readJson(jp)); // IOException, JsonRecordValidateException
                            pageBuilder.setJson(column, value);
                            pageBuilder.addRecord();
                        }
                        catch (IOException | JsonParseException e) {
                            if (stopOnInvalidRecord) {
                                throw new DataException(String.format("Invalid record at line %d: %s", lineNumber, line), e);
                            }
                            log.warn(String.format("Skipped line %d (%s): %s", lineNumber, e.getMessage(), line));
                        }
                    }
                }
                catch (RuntimeException e) {
                    // exception thrown by lines.poll is not retryable
                    throw new DataException(e);
                }
            }
            pageBuilder.finish();
        }
    }

    void doRun(PluginTask task, Schema schema, FileInput input, PageOutput output)
    {
        final FileType fileType = task.getFileType();
        final boolean stopOnInvalidRecord = task.getStopOnInvalidRecord();
        final Column column = schema.getColumn(0); // record column

        try (PageBuilder pageBuilder = newPageBuilder(schema, output);
                FileInputInputStream in = new FileInputInputStream(input)) {
            while (in.nextFile()) {
                try (JsonFileParser fileParser = newJsonFileParser(task, fileType, newJsonParser(in))) {
                    fileParser.skipHeader(); // process file header

                    Value value;
                    while (true) {
                        if ((value = fileParser.nextValue()) == null) {
                            break;
                        }

                        try {
                            pageBuilder.setJson(column, validateValueType(value)); // JsonRecordValidateException
                            pageBuilder.addRecord();
                        }
                        catch (JsonRecordValidateException e) {
                            if (stopOnInvalidRecord) {
                                throw new DataException(String.format("Invalid record at %s", fileParser.getTokenLocation()), e);
                            }
                            log.warn(String.format("Skipped record at %s: %s", fileParser.getTokenLocation(), e.getMessage()));
                        }
                    }

                    fileParser.validateFooter(); // process file footer
                }
                catch (IOException | JsonParseException e) {
                    throw new DataException(e);
                }
            }
            pageBuilder.finish();
        }
    }

    static PageBuilder newPageBuilder(Schema schema, PageOutput output)
    {
        return new PageBuilder(Exec.getBufferAllocator(), schema, output);
    }

    JsonParser newJsonParser(String line)
            throws IOException
    {
        return factory.createParser(line);
    }

    JsonParser newJsonParser(InputStream in)
            throws IOException
    {
        return factory.createParser(in);
    }

    JsonFileParser newJsonFileParser(PluginTask task, FileType fileType, JsonParser jsonParser)
            throws IOException
    {
        switch (fileType) {
        case SEQUENCE:
            return new SequenceParser(jsonParser);
        case OBJECT:
            return new ObjectParser(jsonParser, task.getObjectField().get());
        case ARRAY:
            return new ArrayParser(jsonParser);
        default:
            throw new IllegalStateException(String.format("Unexpected file type: %s", fileType.name));
        }
    }

    Value validateValueType(Value value)
    {
        if (!value.isArrayValue() && !value.isMapValue()) {
            throw new JsonRecordValidateException(String.format("Must be the type of Array or Map: %s", value.toJson()));
        }
        return value;
    }

    static class JsonRecordValidateException
            extends JsonParseException
    {
        JsonRecordValidateException(String message)
        {
            super(message);
        }
    }

    interface JsonFileParser
            extends Closeable
    {
        Value nextValue() throws IOException;

        void skipHeader() throws IOException;
        void validateFooter() throws IOException;

        String getTokenLocation();
    }

    static abstract class AbstractJsonFileParser
            implements JsonFileParser
    {
        protected final FileType fileType;
        protected final JsonParser parser;

        protected AbstractJsonFileParser(FileType fileType, JsonParser parser)
                throws IOException
        {
            this.fileType = fileType;
            this.parser = parser;
        }

        @Override
        public Value nextValue()
                throws IOException
        {
            return JsonUtil.readJson(parser);
        }

        @Override
        public void close()
            throws IOException
        {
            if (parser != null) {
                parser.close();
            }
        }

        @Override
        public String getTokenLocation()
        {
            return parser.getTokenLocation().toString();
        }
    }

    static class SequenceParser
            extends AbstractJsonFileParser
    {
        // {"col":"val1"}{"col":"val2"}
        public SequenceParser(JsonParser parser)
                throws IOException
        {
            super(FileType.SEQUENCE, parser);
        }

        @Override
        public void skipHeader()
                throws IOException
        { }

        @Override
        public void validateFooter()
                throws IOException
        { }
    }

    static class ObjectParser
            extends AbstractJsonFileParser
    {
        private final String fieldName;

        // {"records":[{"col":"val1"},{"col":"val2"},...],...}
        public ObjectParser(JsonParser parser, String fieldName)
                throws IOException
        {
            super(FileType.OBJECT, parser);
            this.fieldName = fieldName;
        }

        @Override
        public void skipHeader()
                throws IOException
        {
            JsonToken token;

            if ((token = parser.nextToken()) == null || !token.equals(JsonToken.START_OBJECT)) {
                throw new JsonParseException(String.format("Unexpected header %s at %s", token, getTokenLocation()));
            }

            while (true) {
                token = parser.nextToken();
                if (token == null) {
                    throw new JsonParseException("Unexpected header");
                }
                String key = parser.getCurrentName();
                if (key == null) {
                    throw new JsonParseException(String.format("Unexpected token %s at %s", token, getTokenLocation()));
                }
                else if (key.equals(fieldName)) {
                    break;
                }
                token = parser.nextToken();
                if (token == null) {
                    throw new JsonParseException("Unexpected header");
                }
            }

            if ((token = parser.nextToken()) == null || !token.equals(JsonToken.START_ARRAY)) {
                throw new JsonParseException(String.format("Unexpected header %s at %s", token, getTokenLocation()));
            }
        }

        @Override
        public void validateFooter()
                throws IOException
        {
            JsonToken token;

            // it should not use nextToken method first. Because the token is already read before.
            if ((token = parser.getCurrentToken()) == null || !token.equals(JsonToken.END_ARRAY)) {
                throw new JsonParseException(String.format("Unexpected footer %s at %s", token, getTokenLocation()));
            }

            while (true) {
                token = parser.nextToken();
                if (token == JsonToken.END_OBJECT) {
                    return;
                }
                else if (token == null) {
                    throw new JsonParseException("Unexpected footer");
                }
                String key = parser.getCurrentName();
                if (key == null) {
                    throw new JsonParseException(String.format("Unexpected token %s at %s", token, getTokenLocation()));
                }
                token = parser.nextToken();
                if (token == null) {
                    throw new JsonParseException("Unexpected footer");
                }
            }
        }
    }

    static class ArrayParser
            extends AbstractJsonFileParser
    {
        // [{"col":"val1"},{"col":"val2"},...]
        public ArrayParser(JsonParser parser)
                throws IOException
        {
            super(FileType.ARRAY, parser);
        }

        @Override
        public void skipHeader()
                throws IOException
        {
            JsonToken token = parser.nextToken();
            if (token == null || !token.equals(JsonToken.START_ARRAY)) {
                throw new JsonParseException(String.format("Unexpected header %s at %s", token, getTokenLocation()));
            }
        }

        @Override
        public void validateFooter()
                throws IOException
        {
            // it should not use nextToken method first. Because the token is already read before.
            JsonToken token = parser.getCurrentToken();
            if (token == null || !token.equals(JsonToken.END_ARRAY)) {
                throw new JsonParseException(String.format("Unexpected footer %s at %s", token, getTokenLocation()));
            }
        }
    }
}
