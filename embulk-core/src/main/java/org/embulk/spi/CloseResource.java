package org.embulk.spi;

import com.google.common.base.Throwables;

public class CloseResource
        implements AutoCloseable
{
    private AutoCloseable resource;

    public CloseResource()
    {
        this(null);
    }

    public CloseResource(AutoCloseable resource)
    {
        this.resource = resource;
    }

    public void closeThis(AutoCloseable resource)
    {
        this.resource = resource;
    }

    public void dontClose()
    {
        this.resource = null;
    }

    public void close()
    {
        if (resource != null) {
            try {
                resource.close();
            }
            catch (Exception ex) {
                throw Throwables.propagate(ex);
            }
        }
    }
}

