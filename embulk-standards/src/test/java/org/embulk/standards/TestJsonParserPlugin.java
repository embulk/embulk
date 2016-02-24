package org.embulk.standards;

import com.google.common.collect.ImmutableList;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.DataException;
import org.embulk.spi.FileInput;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.Schema;
import org.embulk.spi.TestPageBuilderReader.MockPageOutput;
import org.embulk.spi.util.InputStreamFileInput;
import org.embulk.spi.util.Pages;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.msgpack.value.Value;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.msgpack.value.ValueFactory.newArray;
import static org.msgpack.value.ValueFactory.newBoolean;
import static org.msgpack.value.ValueFactory.newInteger;
import static org.msgpack.value.ValueFactory.newMap;
import static org.msgpack.value.ValueFactory.newString;

public class TestJsonParserPlugin
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private ConfigSource config;
    private JsonParserPlugin plugin;
    private MockPageOutput output;

    @Before
    public void createResource()
    {
        config = config();
        plugin = new JsonParserPlugin();
        output = new MockPageOutput();
    }

    @Test
    public void readNormalJson()
            throws Exception
    {
        transaction(config, fileInput(
                "{\"_c0\":true,\"_c1\":10,\"_c2\":\"embulk\",\"_c3\":{\"k\":\"v\"}}",
                "{}",
                "{\n" +
                        "\"_c0\":false,\n" +
                        "\"_c1\":-10,\n" +
                        "\"_c2\":\"エンバルク\",\n" +
                        "\"_c3\":[\"e0\",\"e1\"]\n" +
                "}",
                "[1, 2, 3]", // this line should be skipped.
                "\"embulk\"", // this line should be skipped.
                "10", // this line should be skipped.
                "true", // this line should be skipped.
                "false", // this line should be skipped.
                "null" // this line should be skipped.
        ));

        List<Object[]> records = Pages.toObjects(plugin.newSchema(), output.pages);
        assertEquals(3, records.size());

        Object[] record;
        Map<Value, Value> map;
        { // "{\"_c0\":true,\"_c1\":10,\"_c2\":\"embulk\",\"_c3\":{\"k\":\"v\"}}"
            record = records.get(0);
            assertEquals(1, record.length);
            map = ((Value)record[0]).asMapValue().map();

            assertEquals(newBoolean(true), map.get(newString("_c0")));
            assertEquals(newInteger(10L), map.get(newString("_c1")));
            assertEquals(newString("embulk"), map.get(newString("_c2")));
            assertEquals(newMap(newString("k"), newString("v")), map.get(newString("_c3")));
        }
        { // "{}"
            record = records.get(1);
            assertEquals(1, record.length);
            assertTrue(((Value)record[0]).asMapValue().map().isEmpty());
        }
        {
            record = records.get(2);
            assertEquals(1, record.length);
            map = ((Value)record[0]).asMapValue().map();

            assertEquals(newBoolean(false), map.get(newString("_c0")));
            assertEquals(newInteger(-10L), map.get(newString("_c1")));
            assertEquals(newString("エンバルク"), map.get(newString("_c2")));
            assertEquals(newArray(newString("e0"), newString("e1")), map.get(newString("_c3")));
        }
    }

    @Test
    public void useStopOnInvalidRecord()
            throws Exception
    {
        ConfigSource config = this.config.deepCopy().set("stop_on_invalid_record", true);

        try {
            transaction(config, fileInput(
                    "[1, 2, 3]" // throw DataException
            ));
            fail();
        }
        catch (Throwable t) {
            assertTrue(t instanceof DataException);
        }
    }

    @Test
    public void readBrokenJson()
    {
        try {
            transaction(config, fileInput(
                    "{\"_c0\":true,\"_c1\":10," // throw DataException
            ));
            fail();
        }
        catch (Throwable t) {
            assertTrue(t instanceof DataException);
        }
    }

    private ConfigSource config()
    {
        return runtime.getExec().newConfigSource();
    }

    private void transaction(ConfigSource config, final FileInput input)
    {
        plugin.transaction(config, new ParserPlugin.Control() {
            @Override
            public void run(TaskSource taskSource, Schema schema)
            {
                plugin.run(taskSource, schema, input, output);
            }
        });
    }

    private FileInput fileInput(String... lines)
            throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }

        ByteArrayInputStream in = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
        return new InputStreamFileInput(runtime.getBufferAllocator(), provider(in));
    }

    private InputStreamFileInput.IteratorProvider provider(InputStream... inputStreams)
            throws IOException
    {
        return new InputStreamFileInput.IteratorProvider(
                ImmutableList.copyOf(inputStreams));
    }
}
