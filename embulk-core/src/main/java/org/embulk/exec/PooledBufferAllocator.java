package org.embulk.exec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferAllocator;

public class PooledBufferAllocator implements BufferAllocator {
    public PooledBufferAllocator(final int pageSize) {
        this.pageSize = pageSize;

        // PooledByteBufAllocator(preferDirect = false): buffers are allocated on Java heap.
        this.nettyByteBufAllocator = new PooledByteBufAllocator(false);
    }

    public PooledBufferAllocator() {
        this(DEFAULT_PAGE_SIZE);
    }

    @Override
    public Buffer allocate() {
        return allocate(this.pageSize);
    }

    @Override
    public Buffer allocate(final int minimumCapacity) {
        int size = this.pageSize;
        while (size < minimumCapacity) {
            size *= 2;
        }
        return new BufferBasedOnNettyByteBuf(nettyByteBufAllocator.buffer(size));
    }

    private static class BufferBasedOnNettyByteBuf extends Buffer {
        private BufferBasedOnNettyByteBuf(final ByteBuf internalNettyByteBuf) {
            super(internalNettyByteBuf.array(), internalNettyByteBuf.arrayOffset(), internalNettyByteBuf.capacity());

            this.internalNettyByteBuf = internalNettyByteBuf;
            this.alreadyReleasedAt = null;
        }

        @Override
        public void release() {
            if (this.alreadyReleasedAt != null) {
                new BufferDoubleReleasedException(this.alreadyReleasedAt).printStackTrace();
            }
            if (this.internalNettyByteBuf != null) {
                this.internalNettyByteBuf.release();
                this.internalNettyByteBuf = null;
                this.alreadyReleasedAt = new Throwable();
            }
        }

        private ByteBuf internalNettyByteBuf;
        private Throwable alreadyReleasedAt;
    }

    private static class BufferDoubleReleasedException extends IllegalStateException {
        public BufferDoubleReleasedException(final Throwable alreadyReleasedAt) {
            super("A Buffer detected double release() calls. The buffer has already been released at:", alreadyReleasedAt);
        }
    }

    private static final int DEFAULT_PAGE_SIZE = 32 * 1024;

    private final PooledByteBufAllocator nettyByteBufAllocator;
    private final int pageSize;
}
