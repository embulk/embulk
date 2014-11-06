package org.quickload.spi;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.quickload.buffer.Buffer;
import org.quickload.TestRuntimeModule;
import org.quickload.TestUtilityModule;
import org.quickload.record.RandomSchemaGenerator;
import org.quickload.record.RandomRecordGenerator;

public class TestLineDecoder
{
    public static class TestTask
            implements LineDecoderTask
    {
        private final String encoding;
        private final String newline;

        public TestTask(String encoding, String newline)
        {
            this.encoding = encoding;
            this.newline = newline;
        }

        @Override
        public String getEncoding()
        {
            return encoding;
        }

        @Override
        public String getNewline()
        {
            return newline;
        }

        @Override
        public void validate()
        {
        }
    }

    private static LineDecoder newDecoder(String encoding, String newline,
            List<Buffer> buffers)
    {
        return new LineDecoder(buffers, new TestTask(encoding, newline));
    }

    private static List<Buffer> newBufferList(String encoding, String... sources) throws UnsupportedCharsetException
    {
        Charset charset = Charset.forName(encoding);

        List<Buffer> buffers = new ArrayList<Buffer>();
        for (String source : sources) {
            ByteBuffer buffer = charset.encode(source);
            buffers.add(Buffer.wrap(buffer.array(), buffer.limit()));
        }

        return buffers;
    }

    @Test
    public void testDecodeBasicAscii() throws Exception
    {
        LineDecoder decoder = newDecoder("utf-8", "LF",
                newBufferList("utf-8", "test1\ntest2\ntest3\n"));
        List<String> decoded = ImmutableList.copyOf(decoder);
        assertEquals(Arrays.asList("test1", "test2", "test3"), decoded);
    }
}
