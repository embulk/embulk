package org.embulk.deps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

final class ByteBufferInputStream extends InputStream {
    ByteBufferInputStream(final ByteBuffer buffer, final int begin, final int end) {
        // Creating a duplicated read-only buffer per InputStream instance so that it does not cause any problem
        // even if the buffer is accessed simultaneously from multiple threads.
        this.duplicatedReadOnlyBuffer = buffer.asReadOnlyBuffer();

        // limit() must be set before position().
        // position() satisfies the condition |newPosition| must be less than the current limit.
        this.duplicatedReadOnlyBuffer.limit(end);
        this.duplicatedReadOnlyBuffer.position(begin);
    }

    @Override
    public int read() throws IOException {
        if (!this.duplicatedReadOnlyBuffer.hasRemaining()) {
            return -1;
        }
        return this.duplicatedReadOnlyBuffer.get() & 0xff;
    }

    @Override
    public int read(final byte[] bytes, final int offset, final int desiredLength) throws IOException {
        if (!this.duplicatedReadOnlyBuffer.hasRemaining()) {
            return -1;
        }

        final int remaining = this.duplicatedReadOnlyBuffer.remaining();
        final int actualLength;
        if (desiredLength > remaining) {
            actualLength = remaining;
        } else {
            actualLength = desiredLength;
        }
        this.duplicatedReadOnlyBuffer.get(bytes, offset, actualLength);
        return actualLength;
    }

    @Override
    public long skip(final long desiredLength) throws IOException {
        if (desiredLength <= 0L) {
            return 0L;
        }

        final int remaining = this.duplicatedReadOnlyBuffer.remaining();
        final int actualLength;
        if (desiredLength > (long) remaining) {
            actualLength = remaining;
        } else {
            actualLength = (int) desiredLength;
        }
        this.duplicatedReadOnlyBuffer.position(this.duplicatedReadOnlyBuffer.position() + actualLength);
        return (long) actualLength;
    }

    @Override
    public int available() throws IOException {
        return this.duplicatedReadOnlyBuffer.remaining();
    }

    // No close().

    // No mark(int readlimit).

    // No reset().

    @Override
    public boolean markSupported() {
        return false;
    }

    private final ByteBuffer duplicatedReadOnlyBuffer;
}
