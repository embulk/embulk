package org.embulk.standards;

import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.FormatterPlugin;
import org.embulk.spi.MockFileOutput;
import org.embulk.spi.Page;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageTestUtils;
import org.embulk.spi.Schema;
import org.embulk.spi.time.Timestamp;
import static org.embulk.spi.type.Types.BOOLEAN;
import static org.embulk.spi.type.Types.DOUBLE;
import static org.embulk.spi.type.Types.JSON;
import static org.embulk.spi.type.Types.LONG;
import static org.embulk.spi.type.Types.STRING;
import static org.embulk.spi.type.Types.TIMESTAMP;
import org.embulk.spi.util.Newline;
import org.embulk.spi.util.Pages;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.msgpack.value.ImmutableMapValue;
import static org.msgpack.value.ValueFactory.newBoolean;
import static org.msgpack.value.ValueFactory.newInteger;
import static org.msgpack.value.ValueFactory.newMap;
import static org.msgpack.value.ValueFactory.newString;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestJsonFormatterPlugin
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private JsonFormatterPlugin plugin;
    private MockFileOutput output;

    @Before
    public void createResource()
    {
        plugin = new JsonFormatterPlugin();
        output = new MockFileOutput();
    }

    @Test
    public void checkDefaultConfig()
    {
        ConfigSource config = Exec.newConfigSource();

        JsonFormatterPlugin.PluginTask task = config.loadConfig(JsonFormatterPlugin.PluginTask.class);
        assertEquals(Charset.forName("utf-8"), task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
        assertEquals(DateTimeZone.UTC, task.getDefaultTimeZone());
        assertEquals("%Y-%m-%d %H:%M:%S.%6N %z", task.getDefaultTimestampFormat());
    }

    @Test
    public void checkOptionalConfig()
    {
        ConfigSource config = Exec.newConfigSource()
                .set("charset", "utf-16")
                .set("newline", "LF");

        JsonFormatterPlugin.PluginTask task = config.loadConfig(JsonFormatterPlugin.PluginTask.class);
        assertEquals(Charset.forName("utf-16"), task.getCharset());
        assertEquals(Newline.LF, task.getNewline());
    }

    @Test
    public void testEncodeData()
    {
        ConfigSource config = Exec.newConfigSource()
                .set("charset", "utf-16")
                .set("newline", "LF");
        final Schema schema = getSampleSchema();

        plugin.transaction(config, schema, new FormatterPlugin.Control() {
            @Override
            public void run(TaskSource taskSource)
            {
                PageOutput pageOutput = plugin.open(taskSource, schema, output);
                List<Page> pages = buildPage(schema,
                        123.45, "embulk", 1L, true, Timestamp.ofEpochSecond(1422386629), getSampleJson(),
                        null, null, null, null, null, null
                );
                for (Page page : pages) {
                    pageOutput.add(page);
                }
                pageOutput.finish();
                List<Object[]> records =  Pages.toObjects(schema, pages);
                assertEquals(2, records.size());

                Object[] record = records.get(0);
                assertEquals(123.45, record[0]);
                assertEquals("embulk", record[1]);
                assertEquals(1L, record[2]);
                assertEquals(true, record[3]);
                assertEquals("2015-01-27 19:23:49 UTC", record[4].toString());
                assertEquals("{\"_c1\":true,\"_c2\":10,\"_c3\":\"embulk\",\"_c4\":{\"k\":\"v\"}}", record[5].toString());

                record = records.get(1);
                for (Object col: record) {
                    assertEquals(null, col);
                }
            }
        });
    }

    private List<Page> buildPage(Schema schema, Object... value)
    {
        return PageTestUtils.buildPage(runtime.getBufferAllocator(), schema, value);
    }

    private Schema getSampleSchema()
    {
        return Schema.builder()
                .add("col1", DOUBLE)
                .add("col2", STRING)
                .add("col3", LONG)
                .add("col4", BOOLEAN)
                .add("col5", TIMESTAMP)
                .add("col6", JSON)
                .build();
    }

    private ImmutableMapValue getSampleJson()
    {
        return newMap(
                newString("_c1"), newBoolean(true),
                newString("_c2"), newInteger(10),
                newString("_c3"), newString("embulk"),
                newString("_c4"), newMap(newString("k"), newString("v"))
        );
    }
}
