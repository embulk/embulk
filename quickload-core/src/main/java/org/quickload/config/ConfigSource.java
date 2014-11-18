package org.quickload.config;

import java.lang.reflect.Method;
import com.google.common.base.Optional;
import com.fasterxml.jackson.databind.JsonNode;
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
