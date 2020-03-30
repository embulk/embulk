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
            addSerializer(DataSourceImpl.class, new DataSourceSerializer<DataSourceImpl>());
            addDeserializer(DataSourceImpl.class, new DataSourceDeserializer<DataSourceImpl>(model));

            // ConfigSource
            addSerializer(ConfigSource.class, new DataSourceSerializer<ConfigSource>());
            addDeserializer(ConfigSource.class, new DataSourceDeserializer<ConfigSource>(model));

            // TaskSource
            addSerializer(TaskSource.class, new DataSourceSerializer<TaskSource>());
            addDeserializer(TaskSource.class, new DataSourceDeserializer<TaskSource>(model));

            // TaskReport
            addSerializer(TaskReport.class, new DataSourceSerializer<TaskReport>());
            addDeserializer(TaskReport.class, new DataSourceDeserializer<TaskReport>(model));

            // TODO: Remove this registration by v0.10 or earlier.
            // https://github.com/embulk/embulk/issues/933
            // CommitReport (Deprecated)
            addSerializer(CommitReport.class, new DataSourceSerializer<CommitReport>());
            addDeserializer(CommitReport.class, new DataSourceDeserializer<CommitReport>(model));

            // ConfigDiff
            addSerializer(ConfigDiff.class, new DataSourceSerializer<ConfigDiff>());
            addDeserializer(ConfigDiff.class, new DataSourceDeserializer<ConfigDiff>(model));
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
        @SuppressWarnings("deprecation")  // For the call of getObjectNode().
        @Override
        public void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            if (value instanceof DataSourceImpl) {
                final DataSourceImpl valueImpl = (DataSourceImpl) value;
                valueImpl.getObjectNode().serialize(jgen, provider);
            } else {
                throw new IllegalArgumentException("DataSourceSerDe.DataSourceSerializer#serialize aceepts only DataSourceImpl.");
            }
        }
    }
}
