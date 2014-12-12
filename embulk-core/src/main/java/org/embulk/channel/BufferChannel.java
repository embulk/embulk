package org.embulk.channel;

import org.embulk.buffer.Buffer;

public class BufferChannel
        implements AutoCloseable
{
    private final DataChannel<Buffer> channel;
    private final BufferInput input;
    private final BufferOutput output;

    public BufferChannel(int maxQueuedSize)
    {
        this.channel = new DataChannel<>(maxQueuedSize);
        this.input = new BufferInput(channel);
        this.output = new BufferOutput(channel);
    }

    public BufferInput getInput()
    {
        return input;
    }

    public BufferOutput getOutput()
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
}
