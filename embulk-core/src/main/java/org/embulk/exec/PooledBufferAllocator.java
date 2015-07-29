package org.embulk.exec;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.util.ResourceLeakDetector;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferAllocator;
import com.google.inject.Inject;
import org.embulk.config.ConfigSource;

public class PooledBufferAllocator
        implements BufferAllocator
{
    private static final int DEFAULT_BUFFER_SIZE = 32*1024;
    private static final int MINIMUM_BUFFER_SIZE = 8*1024;

    private final PooledByteBufAllocator nettyBuffer;
    private final int pageSize;

    @Inject
    public PooledBufferAllocator(@ForSystemConfig ConfigSource systemConfig)
    {
        // TODO configure parameters
        this.nettyBuffer = new PooledByteBufAllocator(false);
        // This allows to change page buffer size with -J-Dembulk.pageSize=N option (kB)
        this.pageSize = systemConfig.get(Integer.class, "pageSize", new Integer(MINIMUM_BUFFER_SIZE)).intValue() * 1024;
    }

    public Buffer allocate()
    {
        return allocate(DEFAULT_BUFFER_SIZE);
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
