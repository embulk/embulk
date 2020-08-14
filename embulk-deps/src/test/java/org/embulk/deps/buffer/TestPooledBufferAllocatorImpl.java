package org.embulk.deps.buffer;

import static org.junit.Assert.assertEquals;

import org.embulk.spi.Buffer;
import org.junit.Test;

public class TestPooledBufferAllocatorImpl {
    @Test
    public void testDefault() throws Exception {
        final PooledBufferAllocatorImpl allocator = new PooledBufferAllocatorImpl(4096);
        final Buffer buffer = allocator.allocate();
        assertEquals(0, buffer.offset());
        assertEquals(0, buffer.limit());
        assertEquals(4096, buffer.capacity());
        buffer.release();
    }

    @Test
    public void testWithBiggerMinimumCapacity() throws Exception {
        final PooledBufferAllocatorImpl allocator = new PooledBufferAllocatorImpl(4096);
        final Buffer buffer = allocator.allocate(10000);
        assertEquals(0, buffer.offset());
        assertEquals(0, buffer.limit());
        assertEquals(16384, buffer.capacity());
        buffer.release();
    }

    @Test
    public void testDoubleRelease() throws Exception {
        final PooledBufferAllocatorImpl allocator = new PooledBufferAllocatorImpl(4096);
        final Buffer buffer = allocator.allocate(10000);
        buffer.release();
        buffer.release();  // To printStackTrace of the first release, but no errors.
    }
}
