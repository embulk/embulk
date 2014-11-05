package org.quickload.channel;

import org.quickload.buffer.Buffer;

public class FileBufferChannel
        implements AutoCloseable
{
    private final DataChannel<Buffer> channel;
    private final FileBufferInput input;
    private final FileBufferOutput output;

    public FileBufferChannel(int maxQueuedSize)
    {
        this.channel = new DataChannel(maxQueuedSize);
        this.input = new FileBufferInput(channel);
        this.output = new FileBufferOutput(channel);
    }

    public FileBufferInput getInput()
    {
        return input;
    }

    public FileBufferOutput getOutput()
    {
        return output;
    }

    public void completeProducer()
    {
        channel.completeProducer();
    }

    public void completeConsumer()
    {
        channel.completeConsumer();
    }

    public void join()
    {
        channel.join();
    }

    @Override
    public void close()
    {
        channel.close();
    }

    private static class EndOfFileBuffer
            extends Buffer
    {
        private EndOfFileBuffer()
        {
            super(0);
        }
    }

    public static final Buffer END_OF_FILE = new EndOfFileBuffer();
}
