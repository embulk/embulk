package org.quickload.standards;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.quickload.TestRuntimeBinder;
import org.quickload.TestRuntimeModule;
import org.quickload.buffer.Buffer;
import org.quickload.config.ConfigSource;
import org.quickload.spi.ExecTask;
import org.quickload.spi.LineDecoder;
import static org.quickload.config.DataSource.arrayNode;
import static org.quickload.config.DataSource.objectNode;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestCsvTokenizer
{
    private static CsvTokenizer newTokenizer(CsvParserTask task, List<Buffer> buffers)
    {
        LineDecoder decoder = new LineDecoder(buffers, task);
        return new CsvTokenizer(decoder, task);
    }

    private static List<List<String>> doParse(CsvParserTask task, List<Buffer> buffers)
    {
        return ImmutableList.copyOf(newTokenizer(task, buffers));
    }

    private static List<Buffer> bufferList(String charsetName, String... sources) throws UnsupportedCharsetException
    {
        Charset charset = Charset.forName(charsetName);

        List<Buffer> buffers = new ArrayList<Buffer>();
        for (String source : sources) {
            ByteBuffer buffer = charset.encode(source);
            buffers.add(Buffer.wrap(buffer.array(), buffer.limit()));
        }

        return buffers;
    }

    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    protected ExecTask exec;
    protected ConfigSource config;
    protected CsvParserTask task;

    @Before
    public void setup()
    {
        exec = binder.newExecTask();
        config = new ConfigSource()
                .setString("newline", "LF")
                .set("columns", arrayNode()
                                .add(objectNode()
                                        .put("name", "date_code")
                                        .put("type", "string"))
                                .add(objectNode()
                                        .put("name", "foo")
                                        .put("type", "string"))
                );
        task = exec.loadConfig(config, CsvParserTask.class);
    }

    @Test
    public void parseSimple() throws Exception
    {
        List<List<String>> parsed = doParse(task, bufferList("utf-8",
                "a,b\nc,d\n"));
        assertEquals(Arrays.asList(
                        Arrays.asList("a", "b"),
                        Arrays.asList("c", "d")),
                parsed);
    }
}
