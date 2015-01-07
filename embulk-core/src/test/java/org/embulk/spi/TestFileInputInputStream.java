package org.embulk.spi;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import com.google.common.collect.ImmutableList;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.NextConfig;

public class TestFileInputInputStream
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private MockFileInput fileInput;
    private MockFileOutput fileOutput;

    private FileInputInputStream in;
    private FileOutputOutputStream out;

    private void newOutputStream()
    {
        fileOutput = new MockFileOutput();
        out = new FileOutputOutputStream(fileOutput, runtime.getBufferAllocator());
    }

    private void newInputStream()
    {
        fileInput = new MockFileInput(fileOutput.getFiles());
        in = new FileInputInputStream(fileInput);
    }

    @Test
    public void testRandomReadWrite() throws Exception
    {
        newOutputStream();
        out.nextFile();
        ByteArrayOutputStream expectedOut = new ByteArrayOutputStream();
        Random rand = runtime.getRandom();
        byte[] buffer = new byte[rand.nextInt() % 1024 + 1024];
        for (int i = 0; i < 256; i++) {
            rand.nextBytes(buffer);
            expectedOut.write(buffer);
            out.write(buffer);
        }
        out.finish();
        byte[] expected = expectedOut.toByteArray();
        byte[] actual = new byte[expected.length];

        newInputStream();
        in.nextFile();
        int pos = 0;
        while (pos < actual.length) {
            int n = in.read(actual, pos, actual.length - pos);
            if (n < 0) {
                break;
            }
            pos += n;
        }
        assertEquals(expected.length, pos);
        assertArrayEquals(expected, actual);
    }
}
