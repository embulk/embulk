package org.quickload.standards;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.quickload.TestRuntimeBinder;
import org.quickload.TestRuntimeModule;
import org.quickload.config.ConfigException;
import org.quickload.config.ConfigSource;
import org.quickload.record.ColumnConfig;
import org.quickload.record.SchemaConfig;
import org.quickload.record.Types;
import org.quickload.spi.ExecTask;
import static org.quickload.config.DataSource.arrayNode;
import static org.quickload.config.DataSource.objectNode;

public class TestCsvParserTask
{
    @Rule
    public TestRuntimeBinder binder = new TestRuntimeBinder();

    @Test
    public void checkDefaultValues()
    {
        ExecTask exec = binder.newExecTask();
        ConfigSource config = new ConfigSource()
                .set("columns", arrayNode()
                        .add(objectNode()
                            .put("name", "date_code")
                            .put("type", "string"))
                        );

        CsvParserTask task = exec.loadConfig(config, CsvParserTask.class);
        assertEquals("utf-8", task.getCharset());
        assertEquals("CRLF", task.getNewline());
        assertEquals(false, task.getHeaderLine());
        assertEquals(',', task.getDelimiterChar());
        assertEquals('\"', task.getQuoteChar());
    }

    @Test(expected = ConfigException.class)
    public void checkColumnsRequired()
    {
        ExecTask exec = binder.newExecTask();
        ConfigSource config = new ConfigSource();

        exec.loadConfig(config, CsvParserTask.class);
    }

    @Test
    public void checkLoadConfig()
    {
        ExecTask exec = binder.newExecTask();
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
        assertEquals("utf-16", task.getCharset());
        assertEquals("LF", task.getNewline());
        assertEquals(true, task.getHeaderLine());
        assertEquals('\t', task.getDelimiterChar());
        assertEquals('\\', task.getQuoteChar());
    }
}

