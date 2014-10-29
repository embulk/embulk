package org.quickload.channel;

import org.quickload.record.Page;

public class PageOutput
{
    private final DataChannel<Page> channel;

    PageOutput(DataChannel<Page> channel)
    {
        this.channel = channel;
    }

    public void add(Page page)
    {
        channel.add(page);
    }
}
