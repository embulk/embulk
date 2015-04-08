package org.embulk.exec;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.util.ResourceLeakDetector;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferAllocator;

public class PooledBufferAllocator
        implements BufferAllocator
{
    private static final int DEFAULT_BUFFER_SIZE = 32*1024;
    private static final int MINIMUM_BUFFER_SIZE = 8*1024;

    private final PooledByteBufAllocator nettyBuffer;

    public PooledBufferAllocator()
    {
        // TODO configure parameters
        this.nettyBuffer = new PooledByteBufAllocator(false);
    }

    public Buffer allocate()
    {
        return allocate(DEFAULT_BUFFER_SIZE);
    }

    public Buffer allocate(int minimumCapacity)
    {
        int size = MINIMUM_BUFFER_SIZE;
        while (size < minimumCapacity) {
            size *= 2;
        }
        return new NettyByteBufBuffer(nettyBuffer.buffer(size));
    }

    private static class NettyByteBufBuffer
            extends Buffer
    {
        private ByteBuf buf;
        private BufferReleasedBeforeAt doubleFreeCheck;

        public NettyByteBufBuffer(ByteBuf buf)
        {
            super(buf.array(), buf.arrayOffset(), buf.capacity());
            this.buf = buf;
        }

        public void release()
        {
            if (doubleFreeCheck != null) {
                new BufferDoubleReleasedException(doubleFreeCheck).printStackTrace();
            }
            if (buf != null) {
                buf.release();
                buf = null;
                doubleFreeCheck = new BufferReleasedBeforeAt();
            }
        }
    }

    static class BufferReleasedBeforeAt
            extends Throwable
    { }

    static class BufferDoubleReleasedException
            extends IllegalStateException
    {
        public BufferDoubleReleasedException(BufferReleasedBeforeAt releasedAt)
        {
            super("Detected double release() call of a buffer", releasedAt);
        }
    }
}
