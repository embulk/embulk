package org.embulk.standards;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.nio.charset.Charset;
import org.embulk.TestRuntimeBinder;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.record.ColumnConfig;
import org.embulk.record.SchemaConfig;
import org.embulk.record.Types;
import org.embulk.spi.ExecTask;
import org.embulk.spi.Newline;
import static org.embulk.config.DataSource.arrayNode;
import static org.embulk.config.DataSource.objectNode;

public class TestCsvParserTask
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    protected ExecTask exec;

    @Before
    public void setup()
    {
        exec = binder.newExecTask();
    }

    @Test
    public void checkDefaultValues()
    {
        ConfigSource config = new ConfigSource()
                .set("columns", arrayNode()
                        .add(objectNode()
                            .put("name", "date_code")
                            .put("type", "string"))
                        );

        CsvParserTask task = exec.loadConfig(config, CsvParserTask.class);
        assertEquals(Charset.forName("utf-8"), task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
        assertEquals(false, task.getHeaderLine());
        assertEquals(',', task.getDelimiterChar());
        assertEquals('\"', task.getQuoteChar());
    }

    @Test(expected = ConfigException.class)
    public void checkColumnsRequired()
    {
        ConfigSource config = new ConfigSource();

        exec.loadConfig(config, CsvParserTask.class);
    }

    @Test
    public void checkLoadConfig()
    {
        ConfigSource config = new ConfigSource()
                .setString("charset", "utf-16")
                .setString("newline", "LF")
                .setBoolean("header_line", true)
                .setString("delimiter", "\t")
                .setString("quote", "\\")
                .set("columns", arrayNode()
                        .add(objectNode()
                            .put("name", "date_code")
                            .put("type", "string"))
                        );

        CsvParserTask task = exec.loadConfig(config, CsvParserTask.class);
        assertEquals(Charset.forName("utf-16"), task.getCharset());
        assertEquals(Newline.LF, task.getNewline());
        assertEquals(true, task.getHeaderLine());
        assertEquals('\t', task.getDelimiterChar());
        assertEquals('\\', task.getQuoteChar());
    }
}
