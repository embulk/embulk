package org.quickload.config;

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
}
