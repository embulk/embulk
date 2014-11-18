package org.quickload.standards;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.quickload.GuiceBinder;
import org.quickload.TestRuntimeModule;
import org.quickload.config.ConfigException;
import org.quickload.config.ConfigSource;
import org.quickload.record.ColumnConfig;
import org.quickload.record.SchemaConfig;
import org.quickload.spi.ProcTask;

public class TestCsvParserTask
{
    private static JsonNodeFactory js()
    {
        return JsonNodeFactory.instance;
    }

    @Rule
    public GuiceBinder binder = new GuiceBinder(new TestRuntimeModule());

    @Test
    public void checkDefaultValues()
    {
        ProcTask proc = new ProcTask(binder.getInjector());
        ConfigSource config = new ConfigSource()
                .set("columns", SchemaConfig.columns(js(),
                        ColumnConfig.column(js(), "date_code", "string")));

        CsvParserTask task = proc.loadConfig(config, CsvParserTask.class);
        assertEquals("utf-8", task.getCharset());
        assertEquals("CRLF", task.getNewline());
        assertEquals(false, task.getHeaderLine());
        assertEquals(',', task.getDelimiterChar());
        assertEquals('\"', task.getQuoteChar());
    }

    @Test(expected = ConfigException.class)
    public void checkColumnsRequired()
    {
        ProcTask proc = new ProcTask(binder.getInjector());
        ConfigSource config = new ConfigSource();

        proc.loadConfig(config, CsvParserTask.class);
    }

    @Test
    public void checkLoadConfig()
    {
        ProcTask proc = new ProcTask(binder.getInjector());
        ConfigSource config = new ConfigSource()
                .setString("charset", "utf-16")
                .setString("newline", "LF")
                .setBoolean("header_line", true)
                .setString("delimiter", "\t")
                .setString("quote", "\\")
                .set("columns", SchemaConfig.columns(js(),
                        ColumnConfig.column(js(), "date_code", "string")));

        CsvParserTask task = proc.loadConfig(config, CsvParserTask.class);
        assertEquals("utf-16", task.getCharset());
        assertEquals("LF", task.getNewline());
        assertEquals(true, task.getHeaderLine());
        assertEquals('\t', task.getDelimiterChar());
        assertEquals('\\', task.getQuoteChar());
    }
}

