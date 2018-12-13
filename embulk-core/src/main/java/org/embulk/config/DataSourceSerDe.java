package org.embulk.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

public class DataSourceSerDe {
    public static class SerDeModule extends SimpleModule {
        @SuppressWarnings("deprecation")
        public SerDeModule(final ModelManager model) {
            // DataSourceImpl
            addSerializer(DataSourceImpl.class, new DataSourceSerializer<>());
            addDeserializer(DataSourceImpl.class, new DataSourceDeserializer<>(model));

            // ConfigSource
            addSerializer(ConfigSource.class, new DataSourceSerializer<>());
            addDeserializer(ConfigSource.class, new DataSourceDeserializer<>(model));

            // TaskSource
            addSerializer(TaskSource.class, new DataSourceSerializer<>());
            addDeserializer(TaskSource.class, new DataSourceDeserializer<>(model));

            // TaskReport
            addSerializer(TaskReport.class, new DataSourceSerializer<>());
            addDeserializer(TaskReport.class, new DataSourceDeserializer<>(model));

            // TODO: Remove this registration by v0.10 or earlier.
            // https://github.com/embulk/embulk/issues/933
            // CommitReport (Deprecated)
            addSerializer(CommitReport.class, new DataSourceSerializer<>());
            addDeserializer(CommitReport.class, new DataSourceDeserializer<>(model));

            // ConfigDiff
            addSerializer(ConfigDiff.class, new DataSourceSerializer<>());
            addDeserializer(ConfigDiff.class, new DataSourceDeserializer<>(model));
        }
    }

    // TODO T extends DataSource super DataSourceImpl
    private static class DataSourceDeserializer<T extends DataSource> extends JsonDeserializer<T> {
        private final ModelManager model;
        private final ObjectMapper treeObjectMapper;

        DataSourceDeserializer(ModelManager model) {
            this.model = model;
            this.treeObjectMapper = new ObjectMapper();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode json = treeObjectMapper.readTree(jp);
            if (!json.isObject()) {
                throw new JsonMappingException("Expected object to deserialize DataSource", jp.getCurrentLocation());
            }
            return (T) new DataSourceImpl(model, (ObjectNode) json);
        }
    }

    private static class DataSourceSerializer<T extends DataSource> extends JsonSerializer<T> {
        @Override
        public void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            value.getObjectNode().serialize(jgen, provider);
        }
    }
}
