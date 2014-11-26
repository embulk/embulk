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
import org.quickload.record.Column;
import org.quickload.record.Schema;
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

public class TestCsvTokenizer {
    private static CsvTokenizer newTokenizer(CsvParserTask task, List<Buffer> buffers) {
        LineDecoder decoder = new LineDecoder(buffers, task);
        return new CsvTokenizer(decoder, task);
    }

    private static List<List<String>> doParse(CsvParserTask task, List<Buffer> buffers) {
        final Schema schema = task.getSchemaConfig().toSchema();
        final CsvTokenizer tokenizer = newTokenizer(task, buffers);

        List<List<String>> records = new ArrayList<>();
        while (tokenizer.hasNextRecord()) {
            List<String> record = new ArrayList<>();
            for (Column c : schema.getColumns()) {
                String v = tokenizer.nextColumn();
                if (!v.isEmpty()) {
                    record.add(v);
                } else {
                    record.add(tokenizer.isQuotedColumn() ? "" : null);
                }
            }
            records.add(record);
            tokenizer.nextRecord();
        }
        return records;
    }

    private static List<Buffer> bufferList(String charsetName, String... sources) throws UnsupportedCharsetException {
        Charset charset = Charset.forName(charsetName);

        List<Buffer> buffers = new ArrayList<>();
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
    public void setup() {
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
    public void parseSimple() throws Exception {
        List<List<String>> parsed = doParse(task, bufferList("utf-8",
                "\naaa,bbb\n\nccc,ddd\n"));
        assertEquals(Arrays.asList(
                        Arrays.asList("aaa", "bbb"),
                        Arrays.asList("ccc", "ddd")),
                parsed);
    }

    @Test
    public void parseSeparatedBuffers() throws Exception {
        List<List<String>> parsed = doParse(task, bufferList("utf-8",
                "\naaa", ",bbb\n", "\nccc,ddd", "\n"));
        assertEquals(Arrays.asList(
                        Arrays.asList("aaa", "bbb"),
                        Arrays.asList("ccc", "ddd")),
                parsed);
    }

    @Test
    public void parseQuotedValues() throws Exception {
        List<List<String>> parsed = doParse(task, bufferList("utf-8",
                "\n\"a\r\na\na\"", ",\"b,bb\"\n", "\n", "\"cc\"\"c\",\"\"\"ddd\"", "\n", ",", "\"\"", "\n"));
        assertEquals(Arrays.asList(
                        Arrays.asList("a\r\na\na", "b,bb"),
                        Arrays.asList("ccc", "ddd"), // TODO Arrays.asList("cc\"c", "\"ddd")),
                        Arrays.asList(null, "")),
                parsed);
    }

    @Test
    public void parseTrimedValuesWithNonTriming() throws Exception
    {
        config.setBoolean("trim_if_not_quoted", true);
        task = exec.loadConfig(config, CsvParserTask.class);
        List<List<String>> parsed = doParse(task, bufferList("utf-8",
                "  aaa  ,  b cd", "\n", "\"  ccc\",\"dd d \n \"", "\n"));

        assertEquals(Arrays.asList(
                        Arrays.asList("aaa", "b cd"),
                        Arrays.asList("  ccc","dd d \n ")),
                parsed);
    }

    /*
    @Test(expected = CsvTokenizer.CsvValueValidateException.class)
    public void parseTooLargeSizedValues() throws Exception
    {
        config.setLong("max_quoted_column_size", 8L);
        task = exec.loadConfig(config, CsvParserTask.class);
        List<List<String>> parsed = doParse(task, bufferList("utf-8",
                "aaa,bbb", "\n", "\"cccccccc\",ddd", "\n"));

        assertEquals(Arrays.asList(
                        Arrays.asList("aaa", "bbb"),
                        Arrays.asList("ccc", "ddd")),
                parsed);
    }
    */
    /*
    @Test
    public void parseEscapedQuotedValues() throws Exception
    {
        List<List<String>> parsed = doParse(task, bufferList("utf-8",
                "\"aa,a\",\",aaa\",\"aaa,\"", "\n",
                "\"bb\"\"b\",\"\"\"bbb\",\"bbb\"\"\"", "\n",
                "\"cc\\\"c\",\"\\\"ccc\",\"ccc\\\"\"", "\n",
                "\"dd\nd\",\"\nddd\",\"ddd\n\"", "\n"));
        assertEquals(Arrays.asList(
                        Arrays.asList("aa,a", ",aaa", "aaa,"),
                        Arrays.asList("bb\"b", "\"bbb", "bbb\""),
                        Arrays.asList("cc\"c", "\"ccc", "ccc\""),
                        Arrays.asList("dd\nd", "\nddd", "ddd\n")),
                parsed);
    }
    */
}
