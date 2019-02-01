package org.embulk.standards;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
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
import org.embulk.spi.json.JsonParser;
import org.embulk.spi.type.Types;
import org.embulk.spi.util.FileInputInputStream;
import org.msgpack.core.Preconditions;
import org.msgpack.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonParserPlugin implements ParserPlugin {

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

        @Config("json_pointer_to_root")
        @ConfigDefault("null")
        Optional<String> getJsonPointerToRoot();
    }

    @Override
    public void transaction(ConfigSource configSource, Control control) {
        PluginTask task = configSource.loadConfig(PluginTask.class);
        control.run(task.dump(), newSchema());
    }

    @VisibleForTesting
    Schema newSchema() {
        return Schema.builder().add("record", Types.JSON).build(); // generate a schema
    }

    @Override
    public void run(TaskSource taskSource, Schema schema, FileInput input, PageOutput output) {
        PluginTask task = taskSource.loadTask(PluginTask.class);

        final boolean stopOnInvalidRecord = task.getStopOnInvalidRecord();
        final Column column = schema.getColumn(0); // record column

        try (PageBuilder pageBuilder = newPageBuilder(schema, output);
                FileInputInputStream in = new FileInputInputStream(input)) {
            while (in.nextFile()) {
                final String fileName = input.hintOfCurrentInputFileNameForLogging().orElse("-");

                boolean evenOneJsonParsed = false;
                try (JsonParser.Stream stream = newJsonStream(in, task)) {
                    Value value;
                    while ((value = stream.next()) != null) {
                        try {
                            if (!value.isMapValue()) {
                                throw new JsonRecordValidateException(
                                        String.format("A Json record must not represent map value but it's %s", value.getValueType().name()));
                            }

                            pageBuilder.setJson(column, value);
                            pageBuilder.addRecord();
                            evenOneJsonParsed = true;
                        } catch (JsonRecordValidateException e) {
                            if (stopOnInvalidRecord) {
                                throw new DataException(String.format("Invalid record in %s: %s", fileName, value.toJson()), e);
                            }
                            logger.warn(String.format("Skipped record in %s (%s): %s", fileName, e.getMessage(), value.toJson()));
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

    private PageBuilder newPageBuilder(Schema schema, PageOutput output) {
        return new PageBuilder(Exec.getBufferAllocator(), schema, output);
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

        if (task.getJsonPointerToRoot().isPresent()) {
            return JSON_PARSER.openWithOffsetInJsonPointer(inputStream, task.getJsonPointerToRoot().get());
        } else {
            return JSON_PARSER.open(inputStream);
        }
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

    static class JsonRecordValidateException extends DataException {
        JsonRecordValidateException(String message) {
            super(message);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(JsonParserPlugin.class);

    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\p{XDigit}+");

    private static final JsonParser JSON_PARSER = new JsonParser();
}
