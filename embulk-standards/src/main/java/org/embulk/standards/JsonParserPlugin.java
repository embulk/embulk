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

import java.io.IOException;

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
        final boolean stopOnInvalidRecord = task.getStopOnInvalidRecord();
        final Column column = schema.getColumn(0); // record column

        try (PageBuilder pageBuilder = newPageBuilder(schema, output);
                JsonFileInput in = newJsonFileInput(task, fileType, input)) {
            while (in.nextFile()) {
                try {
                    JsonFileParser parser = newJsonFileParser(factory, task, fileType, in);

                    // read json text before records
                    parser.skipFirst();

                    Value value = null;
                    while (true) {
                        try {
                            value = parser.readJson();
                            if (value == null) {
                                break;
                            }

                            // make sure that it's map value
                            if (!value.isMapValue()) {
                                throw new JsonRecordValidateException(
                                        String.format("A Json record must not represent map value but it's %s", value.getValueType().name()));
                            }

                            pageBuilder.setJson(column, value);
                            pageBuilder.addRecord();
                        }
                        catch (JsonRecordValidateException e) {
                            if (stopOnInvalidRecord) {
                                throw new DataException(String.format("Invalid record: %s", prettyPrint(value)), e);
                            }
                            log.warn(String.format("Skipped record (%s): %s", e.getMessage(), prettyPrint(value)));
                        }
                    }

                    // read json text after records
                    parser.skipLast();
                }
                catch (IOException | JsonParseException e) { // catch JsonFileValidate
                    throw new DataException(e);
                }
            }
            pageBuilder.finish();
        }
    }

    private String prettyPrint(Value v)
    {
        return v != null ? v.toJson() : "null";
    }

    static PageBuilder newPageBuilder(Schema schema, PageOutput output)
    {
        return new PageBuilder(Exec.getBufferAllocator(), schema, output);
    }

    static JsonFileInput newJsonFileInput(PluginTask task, FileType fileType, FileInput in)
    {
        if (fileType.equals(FileType.LINES)) {
            return new JsonLineDecoder(in, task);
        }
        else {
            return new JsonFileInputInputStream(in);
        }
    }

    static JsonFileParser newJsonFileParser(JsonFactory factory, PluginTask task, FileType fileType, JsonFileInput in)
            throws IOException
    {
        if (fileType.equals(FileType.LINES)) {
            return new LinesParser(factory, (JsonLineDecoder)in);
        }

        JsonParser parser = newJsonParser(factory, (JsonFileInputInputStream)in);
        switch (fileType) {
        case SEQUENCE:
            return new SequenceParser(parser);
        case OBJECT:
            return new ObjectParser(parser, task.getObjectField().get());
        case ARRAY:
            return new ArrayParser(parser);
        }

        throw new IllegalArgumentException(String.format("Unexpected file type: %s", fileType.name));
    }

    static JsonParser newJsonParser(JsonFactory factory, JsonFileInputInputStream in)
            throws IOException
    {
        try {
            return factory.createParser(in);
        }
        catch (com.fasterxml.jackson.core.JsonParseException e) {
            throw new JsonParseException("Failed JsonParser object creation", e);
        }
    }

    static Value readJsonObject(JsonParser parser)
            throws IOException
    {
        JsonToken token = parser.nextToken();
        if (token == null || !token.equals(JsonToken.START_OBJECT)) {
            return null;
        }
        else {
            return JsonUtil.jsonTokenToValue(parser, token);
        }
    }

    static class JsonRecordValidateException
            extends JsonParseException
    {
        JsonRecordValidateException(String message)
        {
            super(message);
        }
    }

    static class JsonFileValidateException
            extends JsonParseException
    {
        JsonFileValidateException(String message)
        {
            super(message);
        }
    }

    interface JsonFileInput
            extends AutoCloseable
    {
        boolean nextFile();
        void close();
    }

    static class JsonFileInputInputStream
            extends FileInputInputStream
            implements JsonFileInput
    {
        public JsonFileInputInputStream(FileInput in)
        {
            super(in);
        }
    }

    static class JsonLineDecoder
            extends LineDecoder
            implements JsonFileInput
    {
        private String line = null;
        private int lineNumber = 0;

        public JsonLineDecoder(FileInput in, DecoderTask task)
        {
            super(in, task);
        }

        @Override
        public boolean nextFile()
        {
            boolean next = super.nextFile();
            if (next) {
                lineNumber = 0;
            }
            return next;
        }

        public boolean nextLine()
        {
            line = super.poll();
            boolean next = line != null;
            if (next) {
                lineNumber++;
            }
            return next;
        }

        public String getCurrentLine()
        {
            return line;
        }

        public int getCurrentLineNumber()
        {
            return lineNumber;
        }
    }

    interface JsonFileParser
    {
        Value readJson() throws IOException;
        void skipFirst() throws IOException;
        void skipLast() throws IOException;
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
        public Value readJson()
                throws IOException
        {
            try {
                return readJsonObject(this.parser); // throw JsonParseException
            }
            catch (com.fasterxml.jackson.core.JsonParseException e) {
                throw new JsonParseException("Failed to parse JSON", e);
            }
        }

        protected void throwJsonFileValidateException(String message)
        {
            throw new JsonFileValidateException(newMessage(message));
        }

        private String newMessage(String message)
        {
            return String.format("%s by %s parser at %s", message, fileType, parser.getTokenLocation());
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
        public void skipFirst()
                throws IOException
        {
        }

        @Override
        public void skipLast()
                throws IOException
        {
        }
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
        public void skipFirst()
                throws IOException
        {
            JsonToken token;

            if ((token = parser.nextToken()) == null || !token.equals(JsonToken.START_OBJECT)) {
                throwJsonFileValidateException(String.format("Unexpected head of file: %s is not '{'", token));
            }

            while (true) {
                token = parser.nextToken();
                if (token == null) {
                    throwJsonFileValidateException("Unexpected head of file");
                }
                String key = parser.getCurrentName();
                if (key == null) {
                    throwJsonFileValidateException("Unexpected token " + token);
                }
                else if (key.equals(fieldName)) {
                    break;
                }
                token = parser.nextToken();
                if (token == null) {
                    throwJsonFileValidateException("Unexpected head of file");
                }
            }

            if ((token = parser.nextToken()) == null || !token.equals(JsonToken.START_ARRAY)) {
                throwJsonFileValidateException(String.format("Unexpected head of file: %s is not '['", token));
            }
        }

        @Override
        public void skipLast()
                throws IOException
        {
            JsonToken token;

            // it should not use nextToken method first. Because the token is already read before.
            if ((token = parser.getCurrentToken()) == null || !token.equals(JsonToken.END_ARRAY)) {
                throwJsonFileValidateException(String.format("Unexpected end of file: %s is not ']'", token));
            }

            while (true) {
                token = parser.nextToken();
                if (token == JsonToken.END_OBJECT) {
                    return;
                }
                else if (token == null) {
                    throwJsonFileValidateException(String.format("Unexpected end of file: %s is not '}'", token));
                }
                String key = parser.getCurrentName();
                if (key == null) {
                    throwJsonFileValidateException("Unexpected token " + token);
                }
                token = parser.nextToken();
                if (token == null) {
                    throwJsonFileValidateException("Unexpected end of file");
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
        public void skipFirst()
                throws IOException
        {
            JsonToken token = parser.nextToken();
            if (token == null || !token.equals(JsonToken.START_ARRAY)) {
                throwJsonFileValidateException(String.format("Unexpected head of file: % is not '['", token));
            }
        }

        @Override
        public void skipLast()
                throws IOException
        {
            // it should not use nextToken method first. Because the token is already read before.
            JsonToken token = parser.getCurrentToken();
            if (token == null || !token.equals(JsonToken.END_ARRAY)) {
                throwJsonFileValidateException(String.format("Unexpected end of file: %s is not ']'", token));
            }
        }
    }

    static class LinesParser
            implements JsonFileParser
    {
        private final JsonFactory factory;
        private final JsonLineDecoder input;

        LinesParser(JsonFactory factory, JsonLineDecoder input)
        {
            this.factory = factory;
            this.input = input;
        }

        @Override
        public Value readJson()
                throws IOException
        {
            if (!input.nextLine()) {
                return null;
            }

            try {
                return readJsonObject(factory.createParser(input.getCurrentLine()));
            }
            catch (com.fasterxml.jackson.core.JsonParseException | JsonParseException e) {
                throw new JsonRecordValidateException(String.format("Failed to parse JSON record at line %d: %s", input.getCurrentLineNumber(), input.getCurrentLine()));
            }
        }

        @Override
        public void skipFirst()
                throws IOException
        {
        }

        @Override
        public void skipLast()
                throws IOException
        {
        }
    }
}
