package org.embulk.spi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestBufferImpl {
    @Test
    public void testEquals() throws Exception {
        byte[] bytes = new byte[] { 1, 2, 3, 2, 3 };
        Buffer b1 = BufferImpl.wrap(bytes, 0, 2);  // [1, 2]
        Buffer b2 = BufferImpl.wrap(bytes, 1, 2);  // [2, 3]
        Buffer b3 = BufferImpl.wrap(bytes, 3, 2);  // [2, 3]

        assertFalse(b1.equals(b2));
        assertTrue(b2.equals(b3));

        assertFalse(b1.hashCode() == b2.hashCode());
        assertTrue(b2.hashCode() == b3.hashCode());
    }
}
