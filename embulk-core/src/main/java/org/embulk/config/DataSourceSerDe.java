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
            addDeserializer(ConfigSource.class,
                    new DataSourceDeserializer(new DataSourceFactory<ConfigSource>() {
                        public ConfigSource newInstance(ObjectNode node)
                        {
                            return new ConfigSource(model, node);
                        }
                    }));

            // TaskSource
            addSerializer(TaskSource.class, new DataSourceSerializer<TaskSource>());
            addDeserializer(TaskSource.class,
                    new DataSourceDeserializer(new DataSourceFactory<TaskSource>() {
                        public TaskSource newInstance(ObjectNode node)
                        {
                            return new TaskSource(model, node);
                        }
                    }));

            // CommitReport
            addSerializer(CommitReport.class, new DataSourceSerializer<CommitReport>());
            addDeserializer(CommitReport.class,
                    new DataSourceDeserializer(new DataSourceFactory<CommitReport>() {
                        public CommitReport newInstance(ObjectNode node)
                        {
                            return new CommitReport(model, node);
                        }
                    }));

            // NextConfig
            addSerializer(NextConfig.class, new DataSourceSerializer<NextConfig>());
            addDeserializer(NextConfig.class,
                    new DataSourceDeserializer(new DataSourceFactory<NextConfig>() {
                        public NextConfig newInstance(ObjectNode node)
                        {
                            return new NextConfig(model, node);
                        }
                    }));
        }
    }

    private interface DataSourceFactory <T extends DataSource<T>>
    {
        public T newInstance(ObjectNode node);
    }

    private static class DataSourceDeserializer <T extends DataSource<T>>
            extends JsonDeserializer<T>
    {
        private final DataSourceFactory<T> factory;
        private final ObjectMapper treeObjectMapper;

        DataSourceDeserializer(DataSourceFactory<T> factory)
        {
            this.factory = factory;
            this.treeObjectMapper = new ObjectMapper();
        }

        @Override
        public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            JsonNode json = treeObjectMapper.readTree(jp);
            if (!json.isObject()) {
                throw new JsonMappingException("Expected object to deserialize DataSource", jp.getCurrentLocation());
            }
            return factory.newInstance((ObjectNode) json);
        }
    }

    private static class DataSourceSerializer <T extends DataSource<T>>
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
