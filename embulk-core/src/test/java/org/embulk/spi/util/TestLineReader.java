package org.embulk.spi.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TestLineReader {
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithoutDelimiter() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", null, 256);
        assertEquals(Arrays.asList("test1", "test2", "test3", "test4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterCR() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", LineDelimiter.CR, 256);
        assertEquals(Arrays.asList("test1", "test2\ntest3\r\ntest4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterLF() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", LineDelimiter.LF, 256);
        assertEquals(Arrays.asList("test1\rtest2", "test3\r\ntest4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterCRLF() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", LineDelimiter.CRLF, 256);
        assertEquals(Arrays.asList("test1\rtest2\ntest3", "test4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterAndSmallBuffer() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", LineDelimiter.CR, 1);
        assertEquals(Arrays.asList("test1", "test2\ntest3\r\ntest4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterCRWithEmptyLine() throws IOException {
        List<String> lines = readLines("test1\r\rtest2\r", LineDelimiter.CR, 256);
        assertEquals(Arrays.asList("test1", "", "test2", ""), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterLFWithEmptyLine() throws IOException {
        List<String> lines = readLines("test1\n\ntest2\n", LineDelimiter.LF, 256);
        assertEquals(Arrays.asList("test1", "", "test2", ""), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterCRLFWithEmptyLine() throws IOException {
        List<String> lines = readLines("test1\r\n\r\ntest2\r\n", LineDelimiter.CRLF, 256);
        assertEquals(Arrays.asList("test1", "", "test2", ""), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithoutDelimiterAndEmptyString() throws IOException {
        List<String> lines = readLines("", null, 256);
        assertEquals(Collections.emptyList(), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterAndEmptyString() throws IOException {
        List<String> lines = readLines("", LineDelimiter.CR, 256);
        assertEquals(Collections.emptyList(), lines);
    }

    private static List<String> readLines(String text, LineDelimiter lineDelimiter, int bufferSize) throws IOException {
        LineReader reader = new LineReader(new StringReader(text), lineDelimiter, bufferSize);
        List<String> result = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        return result;
    }
}
