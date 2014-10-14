package org.quickload.config;

import java.io.IOException;
import com.google.inject.Inject;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonMappingException;

public class TaskSourceSerDe
{
    @Inject
    public TaskSourceSerDe(ModelManager modelManager)
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TaskSource.class, new TaskSourceDeserializer(modelManager));
        module.addSerializer(TaskSource.class, new TaskSourceSerializer());
        modelManager.addObjectMapperModule(module);
    }

    // TODO serde for ConfigSource

    private static class TaskSourceDeserializer
            extends JsonDeserializer<TaskSource>
    {
        private final ModelManager modelManager;
        private final ObjectMapper treeObjectMapper;

        TaskSourceDeserializer(ModelManager modelManager)
        {
            this.modelManager = modelManager;
            this.treeObjectMapper = new ObjectMapper();
        }

        @Override
        public TaskSource deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            JsonNode json = treeObjectMapper.readTree(jp);
            if (!json.isObject()) {
                throw new JsonMappingException("Expected object to deserialize TaskSource", jp.getCurrentLocation());
            }
            return new TaskSource(modelManager, (ObjectNode) json);
        }
    }

    private static class TaskSourceSerializer
            extends JsonSerializer<TaskSource>
    {
        @Override
        public void serialize(TaskSource value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException
        {
            value.getData().serialize(jgen, provider);
        }
    }
}
