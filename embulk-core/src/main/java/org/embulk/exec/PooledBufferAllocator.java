package org.embulk.exec;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.ByteBuf;
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

        public NettyByteBufBuffer(ByteBuf buf)
        {
            super(buf.array(), buf.arrayOffset(), buf.capacity());
            this.buf = buf;
        }

        public void release()
        {
            buf.release();
            buf = null;
        }
    }
}
