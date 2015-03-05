package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;

public class MockBufferAllocator implements BufferAllocator {
    List<MockBuffer> allocatedBuffers = new ArrayList<MockBuffer>();

    @Override
    public Buffer allocate() {
        return allocate(1024);
    }

    @Override
    public Buffer allocate(int minimumCapacity) {
        MockBuffer buffer = new MockBuffer(new byte[minimumCapacity], 0, minimumCapacity);
        allocatedBuffers.add(buffer);
        return buffer;
    }

    public List<MockBuffer> getAllocatedBuffers() {
        return allocatedBuffers;
    }
}
