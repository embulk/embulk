package org.quickload.channel;

import org.quickload.record.Page;

public class PageChannel
        implements AutoCloseable
{
    private final DataChannel<Page> channel;
    private final PageInput input;
    private final PageOutput output;

    public PageChannel(int maxQueuedSize)
    {
        this.channel = new DataChannel(maxQueuedSize);
        this.input = new PageInput(channel);
        this.output = new PageOutput(channel);
    }

    public PageInput getInput()
    {
        return input;
    }

    public PageOutput getOutput()
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
