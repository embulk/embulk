package org.quickload.standards;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import org.quickload.config.Task;
import org.quickload.config.TaskSource;
import org.quickload.config.ConfigSource;
import org.quickload.buffer.Buffer;
import org.quickload.buffer.BufferAllocator;
import org.quickload.channel.BufferOutput;
import org.quickload.channel.FileBufferInput;
import org.quickload.channel.FileBufferOutput;
import org.quickload.spi.ProcTask;
import org.quickload.spi.FileEncoderPlugin;

public abstract class OutputStreamFileEncoderPlugin
        implements FileEncoderPlugin
{
    public abstract TaskSource getFileEncoderTask(ProcTask proc, ConfigSource config);

    public abstract OutputStream openOutputStream(ProcTask proc, TaskSource taskSource, OutputStream out) throws IOException;

    public void runFileEncoder(ProcTask proc,
            TaskSource taskSource, int processorIndex,
            FileBufferInput fileBufferInput, FileBufferOutput fileBufferOutput)
    {
        while (fileBufferInput.nextFile()) {
            try (OutputStream out = openOutputStream(proc, taskSource, new BufferedOutputStream(new BufferOutputOutputStream(proc.getBufferAllocator(), fileBufferOutput)))) {
                for (Buffer buffer : fileBufferInput) {
                    out.write(buffer.get(), 0, buffer.limit());
                }
            } catch (IOException ex) {
                // TODO exception class
                throw new RuntimeException(ex);
            }
            // add file after close()
            fileBufferOutput.addFile();
            // skip remaining data
            for (Buffer buffer : fileBufferInput) { }
        }
    }

    private class BufferOutputOutputStream
            extends OutputStream
    {
        private final BufferAllocator allocator;
        private final BufferOutput output;

        // TODO optimize BufferedOutputStream using internal buffering

        public BufferOutputOutputStream(BufferAllocator allocator,
                BufferOutput output)
        {
            this.allocator = allocator;
            this.output = output;
        }

        @Override
        public void write(int b)
        {
            Buffer oneByte = allocator.allocateBuffer(1);
            oneByte.setByte(0, (byte) (b & 0xff));
            oneByte.limit(1);
            output.add(oneByte);
        }

        @Override
        public void write(byte[] b)
        {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len)
        {
            Buffer buffer = allocator.allocateBuffer(len);
            buffer.setBytes(0, b, off, len);
            buffer.limit(len);
            output.add(buffer);
        }

        @Override
        public void flush()
        {
        }

        @Override
        public void close()
        {
        }
    }
}
