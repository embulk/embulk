package org.quickload.channel;

import org.quickload.record.Page;
import java.util.Iterator;

public class PageInput
        implements Iterable<Page>
{
    private final DataChannel<Page> channel;

    PageInput(DataChannel<Page> channel)
    {
        this.channel = channel;
    }

    public Page poll()
    {
        return channel.poll();
    }

    @Override
    public Iterator<Page> iterator()
    {
        return channel.iterator();
    }
}
