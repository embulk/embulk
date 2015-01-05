package org.embulk.config;

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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;

public class DataSourceSerDe
{
    public static class SerDeModule
            extends SimpleModule
    {
        public SerDeModule(final ModelManager model)
        {
            // ConfigSource
            addSerializer(ConfigSource.class, new DataSourceSerializer<ConfigSource>());
            addDeserializer(ConfigSource.class, new DataSourceDeserializer<ConfigSource>(model));

            // TaskSource
            addSerializer(TaskSource.class, new DataSourceSerializer<TaskSource>());
            addDeserializer(TaskSource.class, new DataSourceDeserializer<TaskSource>(model));

            // CommitReport
            addSerializer(CommitReport.class, new DataSourceSerializer<CommitReport>());
            addDeserializer(CommitReport.class, new DataSourceDeserializer<CommitReport>(model));

            // NextConfig
            addSerializer(NextConfig.class, new DataSourceSerializer<NextConfig>());
            addDeserializer(NextConfig.class, new DataSourceDeserializer<NextConfig>(model));
        }
    }

    private static class DataSourceDeserializer <T extends DataSource>  // TODO T extends DataSource super DataSourceImpl
            extends JsonDeserializer<T>
    {
        private final ModelManager model;
        private final ObjectMapper treeObjectMapper;

        DataSourceDeserializer(ModelManager model)
        {
            this.model = model;
            this.treeObjectMapper = new ObjectMapper();
        }

        @Override
        public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            JsonNode json = treeObjectMapper.readTree(jp);
            if (!json.isObject()) {
                throw new JsonMappingException("Expected object to deserialize DataSource", jp.getCurrentLocation());
            }
            return (T) new DataSourceImpl(model, (ObjectNode) json);
        }
    }

    private static class DataSourceSerializer <T extends DataSource>
            extends JsonSerializer<T>
    {
        @Override
        public void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException
        {
            value.getObjectNode().serialize(jgen, provider);
        }
    }
}
