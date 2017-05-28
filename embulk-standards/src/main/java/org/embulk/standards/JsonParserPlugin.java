package org.embulk.standards;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
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
import org.jruby.embed.io.ReaderInputStream;
import org.msgpack.value.Value;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class JsonParserPlugin
        implements ParserPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("stop_on_invalid_record")
        @ConfigDefault("false")
        boolean getStopOnInvalidRecord();

        @Config("clean_illegal_char")
        @ConfigDefault("false")
        boolean getCleanIllegalChar();
    }

    private final Logger log;

    public JsonParserPlugin()
    {
        this.log = Exec.getLogger(JsonParserPlugin.class);
    }

    @Override
    public void transaction(ConfigSource configSource, Control control)
    {
        PluginTask task = configSource.loadConfig(PluginTask.class);
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

        final boolean stopOnInvalidRecord = task.getStopOnInvalidRecord();
        final Column column = schema.getColumn(0); // record column

        try (PageBuilder pageBuilder = newPageBuilder(schema, output);
             FileInputInputStream in = new FileInputInputStream(input)) {
            while (in.nextFile()) {
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
                        }
                        catch (JsonRecordValidateException e) {
                            if (stopOnInvalidRecord) {
                                throw new DataException(String.format("Invalid record: %s", value.toJson()), e);
                            }
                            log.warn(String.format("Skipped record (%s): %s", e.getMessage(), value.toJson()));
                        }
                    }
                }
                catch (IOException | JsonParseException e) {
                    throw new DataException(e);
                }
            }

            pageBuilder.finish();
        }
    }

    private PageBuilder newPageBuilder(Schema schema, PageOutput output)
    {
        return new PageBuilder(Exec.getBufferAllocator(), schema, output);
    }

    private JsonParser.Stream newJsonStream(FileInputInputStream in , PluginTask task)
            throws IOException
    {
        if (task.getCleanIllegalChar())
        {
            final CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder();
            charsetDecoder.onMalformedInput(CodingErrorAction.IGNORE);
            charsetDecoder.onUnmappableCharacter(CodingErrorAction.IGNORE);

            Iterable<CharSource> lines = Lists.transform(CharStreams.readLines(new BufferedReader(new InputStreamReader(in, charsetDecoder))), cleanIllegalBackslashFunction);
            return new JsonParser().open(new ReaderInputStream(CharSource.concat(lines).openStream()));
        }
        else
        {
            return new JsonParser().open(in);
        }
    }

    Function<String, CharSource> cleanIllegalBackslashFunction = new Function<String, CharSource>()
    {
        Pattern p = Pattern.compile("\\p{XDigit}+");
        @Override
        public CharSource apply(@Nullable String input)
        {
            assert input != null;
            int index = 0;
            StringBuilder s = new StringBuilder();
            char[] charArray = input.toCharArray();
            for (char c:charArray) {
                if (c == '\\') {
                    if (charArray.length > index + 1) {
                        char next = charArray[index + 1];
                        switch (next) {
                            case 'b':
                            case 'f':
                            case 'n':
                            case 'r':
                            case 't':
                            case '"':
                            case '\\':
                            case '/':
                                s.append(c);
                                break;
                            case 'u': // hexstring
                                if (charArray.length > index + 5) {
                                    char[] hexChars = { charArray[index + 2] , charArray[index + 3] , charArray[index + 4] ,charArray[index + 5] };
                                    String hexString = new String(hexChars);
                                    if (p.matcher(hexString).matches()) {
                                        s.append(c);
                                    }
                                }
                                break;
                            default:
                                // ignore backslash.
                                break;
                        }
                    }
                } else {
                    s.append(c);
                }
                index++;
            }
            return CharSource.wrap(s.toString());
        }
    };

    static class JsonRecordValidateException
            extends DataException
    {
        JsonRecordValidateException(String message)
        {
            super(message);
        }
    }
}
