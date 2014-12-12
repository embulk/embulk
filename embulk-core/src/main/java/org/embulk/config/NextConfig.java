package org.embulk.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class NextConfig
        extends DataSource<NextConfig>
{
    public NextConfig()
    {
        super();
    }

    /**
     * visible for DataSourceSerDe
     */
    NextConfig(ObjectNode data)
    {
        super(data);
    }

    @Override
    protected NextConfig newInstance(ObjectNode data)
    {
        return new NextConfig(data);
    }
}
