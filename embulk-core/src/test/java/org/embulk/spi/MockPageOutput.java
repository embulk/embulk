package org.embulk.spi;

import java.util.ArrayList;
import java.util.List;

import org.embulk.channel.DataChannel;
import org.embulk.channel.PageOutput;
import org.embulk.record.Page;

import com.google.common.collect.ImmutableList;

public class MockPageOutput
        extends PageOutput
{
    private List<Page> added = new ArrayList<>();
    
    protected MockPageOutput()
    {
        super(new DataChannel<Page>(1));
    }

    @Override
    public void add(Page page)
    {
        added.add(page);
    }
    
    public ImmutableList<Page> getPages() {
        return ImmutableList.copyOf(added);
    }
}
