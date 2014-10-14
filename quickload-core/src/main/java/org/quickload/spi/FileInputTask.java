package org.quickload.spi;

import com.fasterxml.jackson.databind.JsonNode;
import org.quickload.record.Schema;
import org.quickload.config.Config;
import org.quickload.config.TaskSource;

public interface FileInputTask
        extends InputTask
{
    public void setProcessorCount(int c);

    public void setSchema(Schema schema);

    @Config("in:parser_type") // TODO temporarily added 'in:'
    public JsonNode getParserType();

    public TaskSource getParserTask();

    public void setParserTask(TaskSource source);
}
