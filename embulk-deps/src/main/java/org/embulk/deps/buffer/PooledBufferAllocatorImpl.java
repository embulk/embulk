package org.embulk.deps.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferImpl;

public class PooledBufferAllocatorImpl extends org.embulk.exec.PooledBufferAllocator {
    public PooledBufferAllocatorImpl(final int pageSize) {
        this.pageSize = pageSize;

        // PooledByteBufAllocator(preferDirect = false): buffers are allocated on Java heap.
        //
        // Embulk's PooledBufferAllocator respects Netty's default parameters although
        // Netty's default parameters have had some changes in its history.
        //
        // Embulk started to use Netty 4.0.44.Final since Embulk v0.8.18.
        // https://github.com/embulk/embulk/pull/549
        //
        // Netty 4.1.75.Final changed the default chunk size from 16 MiB to 4MiB.
        // https://github.com/netty/netty/pull/12180
        // The default numbers of heap arenas and direct arenas could have been changed along with this change.
        //
        // Netty 4.1.75.Final also changed NOT to use a thread local cache for every thread.
        // https://github.com/netty/netty/pull/12109
        this.nettyByteBufAllocator = new PooledByteBufAllocator(false);
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

    private static class BufferBasedOnNettyByteBuf extends BufferImpl {
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

    private final PooledByteBufAllocator nettyByteBufAllocator;
    private final int pageSize;
}
