package org.embulk.deps.config;

import com.fasterxml.jackson.core.JsonGenerationException;
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
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;

public class DataSourceSerDe {
    public static class SerDeModule extends SimpleModule {
        public SerDeModule(final ModelManagerDelegateImpl model) {
            // DataSourceImpl
            addSerializer(DataSourceImpl.class, new DataSourceSerializer<DataSourceImpl>(model));
            addDeserializer(DataSourceImpl.class, new DataSourceDeserializer<DataSourceImpl>(model));

            // ConfigSource
            addSerializer(ConfigSource.class, new DataSourceSerializer<ConfigSource>(model));
            addDeserializer(ConfigSource.class, new DataSourceDeserializer<ConfigSource>(model));

            // TaskSource
            addSerializer(TaskSource.class, new DataSourceSerializer<TaskSource>(model));
            addDeserializer(TaskSource.class, new DataSourceDeserializer<TaskSource>(model));

            // TaskReport
            addSerializer(TaskReport.class, new DataSourceSerializer<TaskReport>(model));
            addDeserializer(TaskReport.class, new DataSourceDeserializer<TaskReport>(model));

            // ConfigDiff
            addSerializer(ConfigDiff.class, new DataSourceSerializer<ConfigDiff>(model));
            addDeserializer(ConfigDiff.class, new DataSourceDeserializer<ConfigDiff>(model));
        }
    }

    // TODO T extends DataSource super DataSourceImpl
    private static class DataSourceDeserializer<T extends DataSource> extends JsonDeserializer<T> {
        private final ModelManagerDelegateImpl model;

        private final ObjectMapper treeObjectMapper;

        DataSourceDeserializer(ModelManagerDelegateImpl model) {
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
        private final ModelManagerDelegateImpl model;

        DataSourceSerializer(final ModelManagerDelegateImpl model) {
            this.model = model;
        }

        @Override
        public void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            if (value == null) {
                throw new JsonGenerationException(new NullPointerException(
                        "DataSourceSerDe.DataSourceSerializer#serialize accepts only non-null value"));
            }
            final String valueJsonStringified = value.toJson();
            if (valueJsonStringified == null) {
                throw new JsonGenerationException(new NullPointerException(
                        "DataSourceSerDe.DataSourceSerializer#serialize accepts only valid DataSource"));
            }
            final JsonNode valueJsonNode = this.model.readObject(JsonNode.class, valueJsonStringified);
            if (!valueJsonNode.isObject()) {
                throw new JsonGenerationException(new ClassCastException(
                        "DataSourceSerDe.DataSourceSerializer#serialize accepts only valid JSON object"));
            }
            ((ObjectNode) valueJsonNode).serialize(jgen, provider);
        }
    }
}
