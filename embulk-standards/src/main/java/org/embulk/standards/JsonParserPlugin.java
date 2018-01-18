package org.embulk.standards;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
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
    }

    private final Logger log;

    public JsonParserPlugin() {
        this.log = Exec.getLogger(JsonParserPlugin.class);
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
                                throw new DataException(String.format("Invalid record: %s", value.toJson()), e);
                            }
                            log.warn(String.format("Skipped record (%s): %s", e.getMessage(), value.toJson()));
                        }
                    }
                } catch (IOException | JsonParseException e) {
                    if (Exec.isPreview() && evenOneJsonParsed) {
                        // JsonParseException occurs when it cannot parse the last part of sampling buffer. Because
                        // the last part is sometimes invalid as JSON data. Therefore JsonParseException can be
                        // ignore in preview if at least one JSON is already parsed.
                        break;
                    }
                    throw new DataException(e);
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
        InvalidEscapeStringPolicy policy = task.getInvalidEscapeStringPolicy();
        switch (policy) {
            case SKIP:
            case UNESCAPE:
                Iterable<CharSource> lines = Lists.transform(CharStreams.readLines(new BufferedReader(new InputStreamReader(in))),
                        invalidEscapeStringFunction(policy));
                return new JsonParser().open(new ByteArrayInputStream(CharStreams.toString(CharSource.concat(lines).openStream()).getBytes(StandardCharsets.UTF_8)));
            case PASSTHROUGH:
            default:
                return new JsonParser().open(in);
        }
    }

    Function<String, CharSource> invalidEscapeStringFunction(final InvalidEscapeStringPolicy policy) {
        return new Function<String, CharSource>() {
            final Pattern digitsPattern = Pattern.compile("\\p{XDigit}+");

            @Override
            public CharSource apply(@Nullable String input) {
                Preconditions.checkNotNull(input);
                if (policy == InvalidEscapeStringPolicy.PASSTHROUGH) {
                    return CharSource.wrap(input);
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
                                        if (digitsPattern.matcher(hexString).matches()) {
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
                return CharSource.wrap(builder.toString());
            }
        };
    }

    static class JsonRecordValidateException extends DataException {
        JsonRecordValidateException(String message) {
            super(message);
        }
    }
}
