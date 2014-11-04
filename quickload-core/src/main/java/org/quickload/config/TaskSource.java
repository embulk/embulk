package org.quickload.config;

import java.lang.reflect.Method;
import com.google.common.base.Optional;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskSource
{
    protected final ModelManager modelManager;
    private final FieldMapper fieldMapper;
    protected final ObjectNode data;

    public TaskSource(ModelManager modelManager, ObjectNode data)
    {
        this(modelManager, data, null);
    }

    protected TaskSource(ModelManager modelManager, ObjectNode data,
            FieldMapper fieldMapper)
    {
        this.modelManager = modelManager;
        this.data = data;
        this.fieldMapper = fieldMapper;
    }

    public <T extends Task> T loadModel(ModelManager modelManager, Class<T> iface)
    {
        if (fieldMapper == null) {
            return modelManager.readTask(data, iface);
        } else {
            return modelManager.readTask(data, iface, fieldMapper);
        }
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
