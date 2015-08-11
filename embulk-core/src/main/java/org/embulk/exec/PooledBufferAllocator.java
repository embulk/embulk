package org.embulk.exec;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferAllocator;
import com.google.inject.Inject;
import org.embulk.config.ConfigSource;
import org.embulk.spi.unit.ByteSize;

public class PooledBufferAllocator
        implements BufferAllocator
{
    private static final int DEFAULT_PAGE_SIZE = 32*1024;

    private final PooledByteBufAllocator nettyBuffer;
    private final int pageSize;

    @Inject
    public PooledBufferAllocator(@ForSystemConfig ConfigSource systemConfig, org.slf4j.ILoggerFactory factory)
    {
        this.nettyBuffer = new PooledByteBufAllocator(false);
        this.pageSize = systemConfig.get(ByteSize.class, "page_size", new ByteSize(DEFAULT_PAGE_SIZE)).getBytesInt();
    }

    public Buffer allocate()
    {
        return allocate(pageSize);
    }

    public Buffer allocate(int minimumCapacity)
    {
        int size = this.pageSize;
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
