package org.embulk.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConfigSource
        extends DataSource<ConfigSource>
{
    public ConfigSource()
    {
        super();
    }

    ConfigSource(ObjectNode data)
    {
        super(data);
    }

    @Override
    protected ConfigSource newInstance(ObjectNode data)
    {
        return new ConfigSource(data);
    }
}
