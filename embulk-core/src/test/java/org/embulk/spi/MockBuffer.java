package org.embulk.spi;

public class MockBuffer extends Buffer {
    private int releasedCount;

    protected MockBuffer(byte[] wrap, int offset, int capacity) {
        super(wrap, offset, capacity);
    }
    
    public int getReleasedCount() {
        return releasedCount;
    }

    @Override
    public void release() {
        super.release();
        releasedCount++;
    }
}
