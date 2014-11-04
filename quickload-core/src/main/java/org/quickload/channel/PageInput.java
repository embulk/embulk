package org.quickload.channel;

import org.quickload.record.Page;
import java.util.Iterator;

public class PageInput
        implements Iterable<Page>
{
    protected final DataChannel<Page> channel;

    PageInput(DataChannel<Page> channel)
    {
        this.channel = channel;
    }

    @Override
    public Iterator<Page> iterator()
    {
        return channel.iterator();
    }
}
