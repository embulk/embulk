package org.embulk.standards;

import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.nio.charset.Charset;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTimeZone;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Exec;
import org.embulk.spi.util.Newline;

public class TestCsvParserPlugin
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void checkDefaultValues()
    {
        ConfigSource config = Exec.newConfigSource()
            .set("columns", ImmutableList.of(
                        ImmutableMap.of(
                            "name", "date_code",
                            "type", "string"))
                    );

        CsvParserPlugin.PluginTask task = config.loadConfig(CsvParserPlugin.PluginTask.class);
        assertEquals(Charset.forName("utf-8"), task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
        assertEquals(false, task.getHeaderLine().or(false));
        assertEquals(",", task.getDelimiter());
        assertEquals(Optional.of(new CsvParserPlugin.QuoteCharacter('\"')), task.getQuoteChar());
        assertEquals(false, task.getAllowOptionalColumns());
        assertEquals(DateTimeZone.UTC, task.getDefaultTimeZone());
        assertEquals("%Y-%m-%d %H:%M:%S.%N %z", task.getDefaultTimestampFormat());
    }

    @Test(expected = ConfigException.class)
    public void checkColumnsRequired()
    {
        ConfigSource config = Exec.newConfigSource();

        config.loadConfig(CsvParserPlugin.PluginTask.class);
    }

    @Test
    public void checkLoadConfig()
    {
        ConfigSource config = Exec.newConfigSource()
                .set("charset", "utf-16")
                .set("newline", "LF")
                .set("header_line", true)
                .set("delimiter", "\t")
                .set("quote", "\\")
                .set("allow_optional_columns", true)
                .set("columns", ImmutableList.of(
                            ImmutableMap.of(
                                "name", "date_code",
                                "type", "string"))
                        );

        CsvParserPlugin.PluginTask task = config.loadConfig(CsvParserPlugin.PluginTask.class);
        assertEquals(Charset.forName("utf-16"), task.getCharset());
        assertEquals(Newline.LF, task.getNewline());
        assertEquals(true, task.getHeaderLine().or(false));
        assertEquals("\t", task.getDelimiter());
        assertEquals(Optional.of(new CsvParserPlugin.QuoteCharacter('\\')), task.getQuoteChar());
        assertEquals(true, task.getAllowOptionalColumns());
    }
}
