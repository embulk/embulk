package org.embulk.standards;

import com.google.common.annotations.VisibleForTesting;
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
import org.msgpack.value.Value;
import org.slf4j.Logger;

import java.io.IOException;

public class JsonParserPlugin
        implements ParserPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("stop_on_invalid_record")
        @ConfigDefault("false")
        boolean getStopOnInvalidRecord();
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
                try (JsonParser.Stream stream = newJsonStream(in)) {
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

    private JsonParser.Stream newJsonStream(FileInputInputStream in)
            throws IOException
    {
        return new JsonParser().open(in);
    }

    static class JsonRecordValidateException
            extends DataException
    {
        JsonRecordValidateException(String message)
        {
            super(message);
        }
    }
}
