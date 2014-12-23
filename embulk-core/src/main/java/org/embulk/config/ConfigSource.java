package org.embulk.config;

import com.google.inject.Inject;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConfigSource
        extends DataSource<ConfigSource>
{
    @Inject
    public ConfigSource(ModelManager model)
    {
        super(model);
    }

    // visible for DataSourceSerDe and ConfigSourceLoader
    ConfigSource(ModelManager model, ObjectNode data)
    {
        super(model, data);
    }

    @Override
    protected ConfigSource newInstance(ModelManager model, ObjectNode data)
    {
        return new ConfigSource(model, data);
    }

    public <T extends Task> T loadConfig(Class<T> taskType)
    {
        return model.readObjectWithConfigSerDe(taskType, data.traverse());
    }
}
