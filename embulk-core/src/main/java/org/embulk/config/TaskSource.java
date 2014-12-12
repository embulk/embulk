package org.embulk.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskSource
        extends DataSource<TaskSource>
{
    public TaskSource()
    {
        super();
    }

    /**
     * visible for DataSourceSerDe
     */
    TaskSource(ObjectNode data)
    {
        super(data);
    }

    @Override
    protected TaskSource newInstance(ObjectNode data)
    {
        return new TaskSource(data);
    }
}
