package org.embulk.spi;

import java.util.Iterator;

import org.embulk.channel.PageInput;
import org.embulk.record.Page;

public class MockPageInput extends PageInput
{
    private Iterator<Page> iterator;
    
    protected MockPageInput(Iterator<Page> iterator)
    {
        super(null);
        this.iterator = iterator;
    }

    @Override
    public Iterator<Page> iterator()
    {
        return iterator;
    }
}
