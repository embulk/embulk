package org.quickload.config;

import java.lang.reflect.Method;
import com.google.common.base.Optional;
import com.google.common.base.Function;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskSource
{
    protected final ModelManager modelManager;
    private final Function<Method, Optional<String>> jsonKeyMapper;
    protected final ObjectNode data;

    public TaskSource(ModelManager modelManager, ObjectNode data)
    {
        this(modelManager, data, null);
    }

    protected TaskSource(ModelManager modelManager, ObjectNode data,
            Function<Method, Optional<String>> jsonKeyMapper)
    {
        this.modelManager = modelManager;
        this.data = data;
        this.jsonKeyMapper = jsonKeyMapper;
    }

    public <T extends Task> T loadTask(Class<T> iface)
    {
        if (jsonKeyMapper == null) {
            return modelManager.readTask(data, iface);
        } else {
            return modelManager.readTask(data, iface, jsonKeyMapper);
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
