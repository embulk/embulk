package org.quickload.spi;

import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.config.Config;
import org.quickload.config.TaskSource;

public interface FileOutputTask
        extends OutputTask
{
    @Config("out:formatter_type") // TODO temporarily
    public JsonNode getFormatterType();

    public TaskSource getFormatterTask();

    public void setFormatterTask(TaskSource task);
}
