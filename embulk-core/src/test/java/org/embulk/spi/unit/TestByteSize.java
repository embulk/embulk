package org.embulk.spi.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

public class TestByteSize
{
    @Test
    public void testUnitPatterns()
    {
        assertByteSize(42L, "42");
        assertByteSize(42L, "42B");
        assertByteSize(42L*(1L << 10), "42KB");
        assertByteSize(42L*(1L << 20), "42MB");
        assertByteSize(42L*(1L << 30), "42GB");
        assertByteSize(42L*(1L << 40), "42TB");
        assertByteSize(42L*(1L << 50), "42PB");
        assertByteSize(42L, "42 B");
        assertByteSize(42L*(1L << 10), "42 KB");
    }

    @Test
    public void testUnknownUnits()
    {
        assertInvalidByteSize("42XB");
        assertInvalidByteSize("42 XB");
    }

    @Test
    public void testInvalidPatterns()
    {
        assertInvalidByteSize(" 42");
        assertInvalidByteSize("42  B");
        assertInvalidByteSize("42 B ");
        assertInvalidByteSize("42B ");
        assertInvalidByteSize("42  KB");
        assertInvalidByteSize("42 KB ");
        assertInvalidByteSize("42KB ");
    }

    @Test
    public void testInvalidValues()
    {
        assertInvalidByteSize("9223372036854775KB");
    }

    @Test
    public void testToString()
    {
        assertByteSizeString("42B", "42 B");
        assertByteSizeString("42KB", "42 KB");
        assertByteSizeString("42MB", "42 MB");
        assertByteSizeString("42GB", "42 GB");
        assertByteSizeString("42TB", "42 TB");
        assertByteSizeString("42PB", "42 PB");
        assertByteSizeString("42.20KB", "42.2 KB");
        assertByteSizeString("42.33KB", "42.33KB");
    }

    private static void assertByteSize(long bytes, String string)
    {
        assertEquals(bytes, ByteSize.parseByteSize(string).getBytes());
    }

    private static void assertByteSizeString(String expected, String string)
    {
        assertEquals(expected, ByteSize.parseByteSize(string).toString());
    }

    private static void assertInvalidByteSize(String string)
    {
        try {
            ByteSize.parseByteSize(string);
            fail();
        } catch (IllegalArgumentException ex) {
        }
    }
}
