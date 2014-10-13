package org.quickload.config;

import java.lang.reflect.Method;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.quickload.model.ModelManager;

public class TaskSource
        extends AbstractModelSource
{
    public TaskSource(ModelManager modelManager, ObjectNode source)
    {
        super(modelManager, source);
    }
}
