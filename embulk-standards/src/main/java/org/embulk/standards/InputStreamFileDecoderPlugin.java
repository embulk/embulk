package org.embulk.standards;

import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.config.ConfigSource;
import org.embulk.buffer.Buffer;
import org.embulk.channel.FileBufferInput;
import org.embulk.channel.FileBufferOutput;
import org.embulk.spi.ExecTask;
import org.embulk.spi.FileDecoderPlugin;
import org.embulk.spi.FilePlugins;

public abstract class InputStreamFileDecoderPlugin
        implements FileDecoderPlugin
{
    public abstract TaskSource getFileDecoderTask(ExecTask exec, ConfigSource config);

    public abstract InputStream openInputStream(ExecTask exec, TaskSource taskSource, InputStream in) throws IOException;

    @Override
    public void runFileDecoder(ExecTask exec,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, FileBufferOutput fileBufferOutput)
    {
        while (fileBufferInput.nextFile()) {
            try (InputStream in = openInputStream(exec, taskSource, new BufferInputInputStream(fileBufferInput.iterator()))) {
                FilePlugins.transferInputStream(exec.getBufferAllocator(),
                        in, fileBufferOutput);
            } catch (IOException ex) {
                // TODO exception class
                throw new RuntimeException(ex);
            }
            // skip remaining data
            for (Buffer buffer : fileBufferInput) { }
        }
    }

    private class BufferInputInputStream
            extends InputStream
    {
        private final Iterator<Buffer> input;

        private Buffer buffer;
        private int position;

        private byte[] oneByte;  // for read()

        public BufferInputInputStream(Iterator<Buffer> input)
        {
            this.input = input;
        }

        @Override
        public int read()
        {
            if (oneByte == null) {
                oneByte = new byte[1];
            }
            while (true) {
                int n = this.read(oneByte);
                if (n == 1) {
                    return oneByte[0] & 0xff;
                } else if (n < 0) {
                    return -1;
                }
                // n == 0
            }
        }

        @Override
        public int read(byte[] b)
        {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len)
        {
            if (buffer == null) {
                if (!input.hasNext()) {
                    return -1;
                }
                buffer = input.next();
            }

            int remaining = buffer.limit() - position;
            if (remaining <= len) {
                buffer.getBytes(position, b, off, remaining);
                buffer.release();
                buffer = null;
                position = 0;
                return remaining;
            } else {
                buffer.getBytes(position, b, off, len);
                position += len;
                return len;
            }
        }

        @Override
        public void close()
        {
            if (buffer != null) {
                buffer.release();
                buffer = null;
            }
        }
    }
}
