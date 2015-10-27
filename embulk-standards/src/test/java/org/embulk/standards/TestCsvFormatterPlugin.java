package org.embulk.standards;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTimeZone;
import org.junit.Rule;
import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.junit.Assert.assertEquals;
import java.nio.charset.Charset;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Exec;
import org.embulk.spi.util.Newline;

public class TestCsvFormatterPlugin
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Test
    public void checkDefaultValues()
    {
        ConfigSource config = Exec.newConfigSource();

        CsvFormatterPlugin.PluginTask task = config.loadConfig(CsvFormatterPlugin.PluginTask.class);
        assertEquals(Charset.forName("utf-8"), task.getCharset());
        assertEquals(Newline.CRLF, task.getNewline());
        assertEquals(true, task.getHeaderLine());
        assertEquals(',', task.getDelimiterChar());
        assertEquals('\"', task.getQuoteChar());
        assertEquals(CsvFormatterPlugin.QuotePolicy.MINIMAL, task.getQuotePolicy());
        assertEquals(false, task.getEscapeChar().isPresent());
        assertEquals("", task.getNullString());
        assertEquals(DateTimeZone.UTC, task.getDefaultTimeZone());
        assertEquals("%Y-%m-%d %H:%M:%S.%6N %z", task.getDefaultTimestampFormat());
        assertEquals(Newline.LF, task.getNewlineInField());
    }

    @Test
    public void checkLoadConfig()
    {
        ConfigSource config = Exec.newConfigSource()
                .set("charset", "utf-16")
                .set("newline", "LF")
                .set("header_line", false)
                .set("delimiter", "\t")
                .set("quote", "\\")
                .set("quote_policy", "ALL")
                .set("escape", "\"")
                .set("null_string", "\\N")
                .set("newline_in_field", "CRLF");

        CsvFormatterPlugin.PluginTask task = config.loadConfig(CsvFormatterPlugin.PluginTask.class);
        assertEquals(Charset.forName("utf-16"), task.getCharset());
        assertEquals(Newline.LF, task.getNewline());
        assertEquals(false, task.getHeaderLine());
        assertEquals('\t', task.getDelimiterChar());
        assertEquals('\\', task.getQuoteChar());
        assertEquals(CsvFormatterPlugin.QuotePolicy.ALL, task.getQuotePolicy());
        assertEquals('\"', (char) task.getEscapeChar().get());
        assertEquals("\\N", task.getNullString());
        assertEquals(Newline.CRLF, task.getNewlineInField());
    }

    @Test
    public void testQuoteValue()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Method method = CsvFormatterPlugin.class.getDeclaredMethod("setQuoteValue", String.class, char.class);
        method.setAccessible(true);
        CsvFormatterPlugin formatter = new CsvFormatterPlugin();

        assertEquals("\"ABCD\"", method.invoke(formatter, "ABCD", '"'));
        assertEquals("\"\"", method.invoke(formatter, "", '"'));
        assertEquals("'ABCD'", method.invoke(formatter, "ABCD", '\''));
        assertEquals("''", method.invoke(formatter, "", '\''));
    }

    @Test
    public void testEscapeQuote()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Method method = CsvFormatterPlugin.class.getDeclaredMethod("setEscapeAndQuoteValue", String.class, char.class,
                CsvFormatterPlugin.QuotePolicy.class, char.class, char.class, String.class, String.class);
        method.setAccessible(true);
        CsvFormatterPlugin formatter = new CsvFormatterPlugin();

        char delimiter = ',';
        CsvFormatterPlugin.QuotePolicy policy = CsvFormatterPlugin.QuotePolicy.MINIMAL;
        String newline = Newline.LF.getString();

        assertEquals("\"AB\\\"CD\"", method.invoke(formatter, "AB\"CD", delimiter, policy, '"', '\\', newline, ""));
        assertEquals("\"AB\"\"CD\"", method.invoke(formatter, "AB\"CD", delimiter, policy, '"', '"', newline, ""));
    }

    @Test
    public void testQuotePolicyAll()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Method method = CsvFormatterPlugin.class.getDeclaredMethod("setEscapeAndQuoteValue", String.class, char.class,
                CsvFormatterPlugin.QuotePolicy.class, char.class, char.class, String.class, String.class);
        method.setAccessible(true);
        CsvFormatterPlugin formatter = new CsvFormatterPlugin();

        char delimiter = ',';
        char quote = '"';
        char escape = '"';
        CsvFormatterPlugin.QuotePolicy policy = CsvFormatterPlugin.QuotePolicy.ALL;
        String newline = Newline.LF.getString();
        String nullString = "";

        @SuppressWarnings("unchecked")
        ImmutableList<ImmutableMap<String, String>> testCases = ImmutableList.of(
                ImmutableMap.of("expected", "\"true\"", "actual", "true"),
                ImmutableMap.of("expected", "\"false\"", "actual", "false"),
                ImmutableMap.of("expected", "\"0\"", "actual", "0"),
                ImmutableMap.of("expected", "\"1\"", "actual", "1"),
                ImmutableMap.of("expected", "\"1234\"", "actual", "1234"),
                ImmutableMap.of("expected", "\"-1234\"", "actual", "-1234"),
                ImmutableMap.of("expected", "\"+1234\"", "actual", "+1234"),
                ImmutableMap.of("expected", "\"0x4d2\"", "actual", "0x4d2"),
                ImmutableMap.of("expected", "\"123L\"", "actual", "123L"),
                ImmutableMap.of("expected", "\"3.141592\"", "actual", "3.141592"),
                ImmutableMap.of("expected", "\"1,000\"", "actual", "1,000"),
                ImmutableMap.of("expected", "\"ABC\"", "actual", "ABC"),
                ImmutableMap.of("expected", "\"ABC\"\"DEF\"", "actual", "ABC\"DEF"),
                ImmutableMap.of("expected", "\"ABC\nDEF\"", "actual", "ABC\nDEF"),
                ImmutableMap.of("expected", "\"\"", "actual", ""),
                ImmutableMap.of("expected", "\"NULL\"", "actual", "NULL"),
                ImmutableMap.of("expected", "\"2015-01-01 12:01:01\"", "actual", "2015-01-01 12:01:01"),
                ImmutableMap.of("expected", "\"20150101\"", "actual", "20150101"));

        for (ImmutableMap testCase : testCases) {
            String expected = (String) testCase.get("expected");
            String actual = (String) testCase.get("actual");
            assertEquals(expected, method.invoke(formatter, actual, delimiter, policy, quote, escape, newline, nullString));
        }
    }

    @Test
    public void testQuotePolicyMinimal()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Method method = CsvFormatterPlugin.class.getDeclaredMethod("setEscapeAndQuoteValue", String.class, char.class,
                CsvFormatterPlugin.QuotePolicy.class, char.class, char.class, String.class, String.class);
        method.setAccessible(true);
        CsvFormatterPlugin formatter = new CsvFormatterPlugin();

        char delimiter = ',';
        char quote = '"';
        char escape = '"';
        CsvFormatterPlugin.QuotePolicy policy = CsvFormatterPlugin.QuotePolicy.MINIMAL;
        String newline = Newline.LF.getString();
        String nullString = "";

        @SuppressWarnings("unchecked")
        ImmutableList<ImmutableMap<String, String>> testCases = ImmutableList.of(
                ImmutableMap.of("expected", "true", "actual", "true"),
                ImmutableMap.of("expected", "false", "actual", "false"),
                ImmutableMap.of("expected", "0", "actual", "0"),
                ImmutableMap.of("expected", "1", "actual", "1"),
                ImmutableMap.of("expected", "1234", "actual", "1234"),
                ImmutableMap.of("expected", "-1234", "actual", "-1234"),
                ImmutableMap.of("expected", "+1234", "actual", "+1234"),
                ImmutableMap.of("expected", "0x4d2", "actual", "0x4d2"),
                ImmutableMap.of("expected", "123L", "actual", "123L"),
                ImmutableMap.of("expected", "3.141592", "actual", "3.141592"),
                ImmutableMap.of("expected", "\"1,000\"", "actual", "1,000"),
                ImmutableMap.of("expected", "ABC", "actual", "ABC"),
                ImmutableMap.of("expected", "\"ABC\"\"DEF\"", "actual", "ABC\"DEF"),
                ImmutableMap.of("expected", "\"ABC\nDEF\"", "actual", "ABC\nDEF"),
                ImmutableMap.of("expected", "\"\"", "actual", ""),
                ImmutableMap.of("expected", "NULL", "actual", "NULL"),
                ImmutableMap.of("expected", "2015-01-01 12:01:01", "actual", "2015-01-01 12:01:01"),
                ImmutableMap.of("expected", "20150101", "actual", "20150101"));

        for (ImmutableMap testCase : testCases) {
            String expected = (String) testCase.get("expected");
            String actual = (String) testCase.get("actual");
            assertEquals(expected, method.invoke(formatter, actual, delimiter, policy, quote, escape, newline, nullString));
        }
    }

    @Test
    public void testQuotePolicyNone()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Method method = CsvFormatterPlugin.class.getDeclaredMethod("setEscapeAndQuoteValue", String.class, char.class,
                CsvFormatterPlugin.QuotePolicy.class, char.class, char.class, String.class, String.class);
        method.setAccessible(true);
        CsvFormatterPlugin formatter = new CsvFormatterPlugin();

        char delimiter = ',';
        char quote = '"';
        char escape = '\\';
        CsvFormatterPlugin.QuotePolicy policy = CsvFormatterPlugin.QuotePolicy.NONE;
        String newline = Newline.LF.getString();
        String nullString = "";

        @SuppressWarnings("unchecked")
        ImmutableList<ImmutableMap<String, String>> testCases = ImmutableList.of(
                ImmutableMap.of("expected", "true", "actual", "true"),
                ImmutableMap.of("expected", "false", "actual", "false"),
                ImmutableMap.of("expected", "0", "actual", "0"),
                ImmutableMap.of("expected", "1", "actual", "1"),
                ImmutableMap.of("expected", "1234", "actual", "1234"),
                ImmutableMap.of("expected", "-1234", "actual", "-1234"),
                ImmutableMap.of("expected", "+1234", "actual", "+1234"),
                ImmutableMap.of("expected", "0x4d2", "actual", "0x4d2"),
                ImmutableMap.of("expected", "123L", "actual", "123L"),
                ImmutableMap.of("expected", "3.141592", "actual", "3.141592"),
                ImmutableMap.of("expected", "1\\,000", "actual", "1,000"),
                ImmutableMap.of("expected", "ABC", "actual", "ABC"),
                ImmutableMap.of("expected", "ABC\"DEF", "actual", "ABC\"DEF"),
                ImmutableMap.of("expected", "ABC\\\nDEF", "actual", "ABC\nDEF"),
                ImmutableMap.of("expected", "", "actual", ""),
                ImmutableMap.of("expected", "NULL", "actual", "NULL"),
                ImmutableMap.of("expected", "2015-01-01 12:01:01", "actual", "2015-01-01 12:01:01"),
                ImmutableMap.of("expected", "20150101", "actual", "20150101"));

        for (ImmutableMap testCase : testCases) {
            String expected = (String) testCase.get("expected");
            String actual = (String) testCase.get("actual");
            assertEquals(expected, method.invoke(formatter, actual, delimiter, policy, quote, escape, newline, nullString));
        }
    }

    @Test
    public void testNewlineInField()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Method method = CsvFormatterPlugin.class.getDeclaredMethod("setEscapeAndQuoteValue", String.class, char.class,
                CsvFormatterPlugin.QuotePolicy.class, char.class, char.class, String.class, String.class);
        method.setAccessible(true);
        CsvFormatterPlugin formatter = new CsvFormatterPlugin();

        char delimiter = ',';
        char quote = '"';
        char escape = '"';
        String newline;
        CsvFormatterPlugin.QuotePolicy policy = CsvFormatterPlugin.QuotePolicy.MINIMAL;
        String nullString = "";

        ImmutableList<ImmutableMap<String, String>> testCases;

        newline = Newline.LF.getString();
        testCases = ImmutableList.of(
                ImmutableMap.of("expected", "\"ABC\nDEF\"", "actual", "ABC\r\nDEF"),
                ImmutableMap.of("expected", "\"ABC\nDEF\"", "actual", "ABC\rDEF"),
                ImmutableMap.of("expected", "\"ABC\nDEF\"", "actual", "ABC\nDEF"));

        for (ImmutableMap testCase : testCases) {
            String expected = (String) testCase.get("expected");
            String actual = (String) testCase.get("actual");
            assertEquals(expected, method.invoke(formatter, actual, delimiter, policy, quote, escape, newline, nullString));
        }


        newline = Newline.CRLF.getString();
        testCases = ImmutableList.of(
                ImmutableMap.of("expected", "\"ABC\r\nDEF\"", "actual", "ABC\r\nDEF"),
                ImmutableMap.of("expected", "\"ABC\r\nDEF\"", "actual", "ABC\rDEF"),
                ImmutableMap.of("expected", "\"ABC\r\nDEF\"", "actual", "ABC\nDEF"));

        for (ImmutableMap testCase : testCases) {
            String expected = (String) testCase.get("expected");
            String actual = (String) testCase.get("actual");
            assertEquals(expected, method.invoke(formatter, actual, delimiter, policy, quote, escape, newline, nullString));
        }


        newline = Newline.CR.getString();
        testCases = ImmutableList.of(
                ImmutableMap.of("expected", "\"ABC\rDEF\"", "actual", "ABC\r\nDEF"),
                ImmutableMap.of("expected", "\"ABC\rDEF\"", "actual", "ABC\rDEF"),
                ImmutableMap.of("expected", "\"ABC\rDEF\"", "actual", "ABC\nDEF"));

        for (ImmutableMap testCase : testCases) {
            String expected = (String) testCase.get("expected");
            String actual = (String) testCase.get("actual");
            assertEquals(expected, method.invoke(formatter, actual, delimiter, policy, quote, escape, newline, nullString));
        }
    }

    @Test
    public void testNullString()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Method method = CsvFormatterPlugin.class.getDeclaredMethod("setEscapeAndQuoteValue", String.class, char.class,
                CsvFormatterPlugin.QuotePolicy.class, char.class, char.class, String.class, String.class);
        method.setAccessible(true);
        CsvFormatterPlugin formatter = new CsvFormatterPlugin();

        char delimiter = ',';
        char quote = '"';
        char escape = '"';
        CsvFormatterPlugin.QuotePolicy policy = CsvFormatterPlugin.QuotePolicy.MINIMAL;
        String newline = Newline.LF.getString();

        assertEquals("\"\"", method.invoke(formatter, "", delimiter, CsvFormatterPlugin.QuotePolicy.MINIMAL, quote, escape, newline, ""));
        assertEquals("N/A", method.invoke(formatter, "N/A", delimiter, CsvFormatterPlugin.QuotePolicy.MINIMAL, quote, escape, newline, ""));
        assertEquals("", method.invoke(formatter, "", delimiter, CsvFormatterPlugin.QuotePolicy.NONE, quote, escape, newline, ""));
        assertEquals("N/A", method.invoke(formatter, "N/A", delimiter, CsvFormatterPlugin.QuotePolicy.NONE, quote, escape, newline, ""));

        assertEquals("", method.invoke(formatter, "", delimiter, CsvFormatterPlugin.QuotePolicy.MINIMAL, quote, escape, newline, "N/A"));
        assertEquals("\"N/A\"", method.invoke(formatter, "N/A", delimiter, CsvFormatterPlugin.QuotePolicy.MINIMAL, quote, escape, newline, "N/A"));
        assertEquals("", method.invoke(formatter, "", delimiter, CsvFormatterPlugin.QuotePolicy.NONE, quote, escape, newline, "N/A"));
        assertEquals("N/A", method.invoke(formatter, "N/A", delimiter, CsvFormatterPlugin.QuotePolicy.NONE, quote, escape, newline, "N/A"));
    }
}
