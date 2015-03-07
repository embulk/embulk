package org.embulk.exec;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.util.ResourceLeakDetector;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferAllocator;

public class PooledBufferAllocator
        implements BufferAllocator
{
    private PooledByteBufAllocator nettyBuffer;

    public PooledBufferAllocator()
    {
        // TODO configure parameters
        this.nettyBuffer = new PooledByteBufAllocator(false);
    }

    public Buffer allocate()
    {
        return new NettyByteBufBuffer(nettyBuffer.buffer());
    }

    public Buffer allocate(int minimumCapacity)
    {
        int size = 32*1024;
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
