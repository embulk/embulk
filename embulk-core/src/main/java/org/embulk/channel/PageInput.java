package org.embulk.channel;

import org.embulk.record.Page;
import java.util.Iterator;

public class PageInput
        implements Iterable<Page>
{
    protected final DataChannel<Page> channel;

    protected PageInput(DataChannel<Page> channel)
    {
        this.channel = channel;
    }

    //public static interface Listener
    //        extends DataChannel.Listener <Page>
    //{
    //    public void add(Page e);
    //}
    //
    //public void processByListener(Listener listener)
    //{
    //    channel.setListener(listener);
    //    channel.join();
    //}

    @Override
    public Iterator<Page> iterator()
    {
        return channel.iterator();
    }
}
