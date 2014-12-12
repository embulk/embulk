package org.embulk.channel;

import org.embulk.record.Page;

public class PageOutput
{
    protected final DataChannel<Page> channel;

    protected PageOutput(DataChannel<Page> channel)
    {
        this.channel = channel;
    }

    public void add(Page page)
    {
        channel.add(page);
    }
}
