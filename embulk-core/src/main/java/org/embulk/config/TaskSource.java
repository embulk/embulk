package org.embulk.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskSource
        extends DataSource<TaskSource>
{
    public TaskSource(ModelManager model)
    {
        super(model);
    }

    // visible for DataSourceSerDe and TaskInvocationHandler.dump
    TaskSource(ModelManager model, ObjectNode data)
    {
        super(model, data);
    }

    @Override
    protected TaskSource newInstance(ModelManager model, ObjectNode data)
    {
        return new TaskSource(model, data);
    }

    public <T extends Task> T loadTask(Class<T> taskType)
    {
        return model.readObject(taskType, data.traverse());
    }
}
