package org.quickload.spi;

import java.util.Iterator;

import org.quickload.channel.PageInput;
import org.quickload.record.Page;

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
