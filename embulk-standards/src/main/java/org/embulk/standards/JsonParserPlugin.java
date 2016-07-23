package org.embulk.standards;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonParseException;
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
import org.embulk.spi.json.JsonUtil;
import org.embulk.spi.type.Types;
import org.embulk.spi.util.FileInputInputStream;
import org.embulk.spi.util.LineDecoder;
import org.msgpack.value.Value;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Locale.ENGLISH;

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
            throw new ConfigException(String.format(ENGLISH,
                        "Unknown FileType '%s'. Available options are [sequence, object, array, lines]", name));
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
            // parseLines can recover from parsing error by skipping a line
            parseLines(task, schema, input, output);
            break;
        default:
            // parseFile can't recover from parsing error
            parseFile(task, schema, input, output);
            break;
        }
    }

    private void parseLines(PluginTask task, Schema schema, FileInput input, PageOutput output)
    {
        final boolean stopOnInvalidRecord = task.getStopOnInvalidRecord();
        final Column column = schema.getColumn(0); // record column

        try (PageBuilder pageBuilder = newPageBuilder(schema, output)) {
            while (input.nextFile()) {
                int lineNumber = 0;
                try (LineDecoder lines = new LineDecoder(input, task)) {
                    while (true) {
                        String line;
                        try {
                            line = lines.poll();
                        }
                        catch (RuntimeException ex) {
                            // exception thrown by lines.poll is not retryable
                            throw new DataException(ex);
                        }
                        if (line == null) {
                            break;
                        }
                        lineNumber++;

                        Value value;
                        try (JsonParser jp = factory.createParser(line)) {
                            value = JsonUtil.readJson(jp);
                            validateValueType(value);
                        }
                        catch (IOException | JsonRecordValidateException e) {
                            if (stopOnInvalidRecord) {
                                throw new DataException(String.format(ENGLISH,
                                            "Invalid record at line %d: %s", lineNumber, line), e);
                            }
                            log.warn(String.format(ENGLISH,
                                        "Skipped line %d (%s): %s", lineNumber, e.getMessage(), line));
                            continue;
                        }

                        pageBuilder.setJson(column, value);
                        pageBuilder.addRecord();
                    }
                }
            }
            pageBuilder.finish();
        }
    }

    private void parseFile(PluginTask task, Schema schema, FileInput input, PageOutput output)
    {
        final FileType fileType = task.getFileType();
        final boolean stopOnInvalidRecord = task.getStopOnInvalidRecord();
        final Column column = schema.getColumn(0); // record column

        try (PageBuilder pageBuilder = newPageBuilder(schema, output);
                FileInputInputStream in = new FileInputInputStream(input)) {
            while (in.nextFile()) {
                try (JacksonFileParser fileParser = newFileParser(task, fileType, factory.createParser(in))) {
                    fileParser.skipHeader(); // process file header

                    while (true) {
                        Value value;
                        if ((value = fileParser.nextValue()) == null) {
                            break;
                        }

                        try {
                            validateValueType(value);
                            pageBuilder.setJson(column, value);
                            pageBuilder.addRecord();
                        }
                        catch (JsonRecordValidateException e) {
                            if (stopOnInvalidRecord) {
                                throw new DataException(String.format(ENGLISH,
                                            "Invalid record at %s", fileParser.getTokenLocation()), e);
                            }
                            log.warn(String.format(ENGLISH,
                                        "Skipped record at %s: %s", fileParser.getTokenLocation(), e.getMessage()));
                        }
                    }

                    fileParser.validateFooter(); // process file footer
                }
                catch (IOException | JsonFileException e) {
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

    private JacksonFileParser newFileParser(PluginTask task, FileType fileType, JsonParser jsonParser)
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

    private void validateValueType(Value value)
        throws JsonRecordValidateException
    {
        if (value.isArrayValue() || value.isMapValue()) {
            return;
        }
        else {
            throw new JsonRecordValidateException(String.format(ENGLISH,
                        "Must be the type of Array or Map: %s", value.toJson()));
        }
    }

    static class JsonRecordValidateException
            extends Exception
    {
        JsonRecordValidateException(String message)
        {
            super(message);
        }
    }

    static class JsonFileException
            extends Exception
    {
        JsonFileException(String message)
        {
            super(message);
        }
    }

    static abstract class JacksonFileParser
            implements Closeable
    {
        protected final FileType fileType;
        protected final JsonParser parser;

        protected JacksonFileParser(FileType fileType, JsonParser parser)
                throws IOException
        {
            this.fileType = fileType;
            this.parser = parser;
        }

        public abstract void skipHeader() throws IOException, JsonFileException;

        public abstract void validateFooter() throws IOException, JsonFileException;

        public Value nextValue()
                throws IOException
        {
            return JsonUtil.readJson(parser);
        }

        public String getTokenLocation()
        {
            return parser.getTokenLocation().toString();
        }

        @Override
        public void close()
            throws IOException
        {
            if (parser != null) {
                parser.close();
            }
        }
    }

    static class SequenceParser
            extends JacksonFileParser
    {
        // {"col":"val1"}{"col":"val2"}
        public SequenceParser(JsonParser parser)
                throws IOException
        {
            super(FileType.SEQUENCE, parser);
        }

        @Override
        public void skipHeader()
                throws IOException, JsonFileException
        { }

        @Override
        public void validateFooter()
                throws IOException, JsonFileException
        { }
    }

    static class ObjectParser
            extends JacksonFileParser
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
                throws IOException, JsonFileException
        {
            JsonToken token;

            if ((token = parser.nextToken()) == null || !token.equals(JsonToken.START_OBJECT)) {
                throw new JsonFileException(String.format(ENGLISH,
                            "Unexpected header %s at %s", token, getTokenLocation()));
            }

            while (true) {
                token = parser.nextToken();
                if (token == null) {
                    throw new JsonFileException("Unexpected header");
                }
                String key = parser.getCurrentName();
                if (key == null) {
                    throw new JsonFileException(String.format(ENGLISH,
                                "Unexpected token %s at %s", token, getTokenLocation()));
                }
                else if (key.equals(fieldName)) {
                    break;
                }
                token = parser.nextToken();
                if (token == null) {
                    throw new JsonFileException("Unexpected header");
                }
            }

            if ((token = parser.nextToken()) == null || !token.equals(JsonToken.START_ARRAY)) {
                throw new JsonFileException(String.format(ENGLISH,
                            "Unexpected header %s at %s", token, getTokenLocation()));
            }
        }

        @Override
        public void validateFooter()
                throws IOException, JsonFileException
        {
            JsonToken token;

            // it should not use nextToken method first. Because the token is already read before.
            if ((token = parser.getCurrentToken()) == null || !token.equals(JsonToken.END_ARRAY)) {
                throw new JsonFileException(String.format(ENGLISH,
                            "Unexpected footer %s at %s", token, getTokenLocation()));
            }

            while (true) {
                token = parser.nextToken();
                if (token == JsonToken.END_OBJECT) {
                    return;
                }
                else if (token == null) {
                    throw new JsonFileException("Unexpected footer");
                }
                String key = parser.getCurrentName();
                if (key == null) {
                    throw new JsonFileException(String.format(ENGLISH,
                                "Unexpected token %s at %s", token, getTokenLocation()));
                }
                token = parser.nextToken();
                if (token == null) {
                    throw new JsonFileException("Unexpected footer");
                }
            }
        }
    }

    static class ArrayParser
            extends JacksonFileParser
    {
        // [{"col":"val1"},{"col":"val2"},...]
        public ArrayParser(JsonParser parser)
                throws IOException
        {
            super(FileType.ARRAY, parser);
        }

        @Override
        public void skipHeader()
                throws IOException, JsonFileException
        {
            JsonToken token = parser.nextToken();
            if (token == null || !token.equals(JsonToken.START_ARRAY)) {
                throw new JsonFileException(String.format(ENGLISH,
                            "Unexpected header %s at %s", token, getTokenLocation()));
            }
        }

        @Override
        public void validateFooter()
                throws IOException, JsonFileException
        {
            // it should not use nextToken method first. Because the token is already read before.
            JsonToken token = parser.getCurrentToken();
            if (token == null || !token.equals(JsonToken.END_ARRAY)) {
                throw new JsonFileException(String.format(ENGLISH,
                            "Unexpected footer %s at %s", token, getTokenLocation()));
            }
        }
    }
}
