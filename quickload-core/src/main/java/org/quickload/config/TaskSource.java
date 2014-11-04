package org.quickload.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskSource
        extends DataSource<TaskSource>
{
    protected final ModelManager modelManager;

    public TaskSource(ModelManager modelManager, ObjectNode data)
    {
        this(modelManager, data, null);
    }

    protected TaskSource(ModelManager modelManager, ObjectNode data,
            FieldMapper fieldMapper)
    {
        super(data, fieldMapper);
        this.modelManager = modelManager;
    }

    @Deprecated
    public <T extends Task> T loadTask(Class<T> iface)
    {
        if (fieldMapper == null) {
            return modelManager.readTask(data, iface);
        } else {
            return modelManager.readTask(data, iface, fieldMapper);
        }
    }

    // This is a utility method
    public TaskSource dumpTask(Task task)
    {
        task.validate();
        return modelManager.readJsonObject(
                modelManager.writeJsonObjectNode(task),
                TaskSource.class);
    }

    /**
     * visible for TaskSourceSerDe
     */
    ObjectNode getData()
    {
        return data;
    }

    @Override
    public String toString()
    {
        return data.toString();
    }
}
