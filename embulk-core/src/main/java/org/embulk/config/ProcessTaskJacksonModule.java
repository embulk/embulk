package org.embulk.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.embulk.plugin.PluginType;
import org.embulk.spi.ProcessTask;
import org.embulk.spi.Schema;

final class ProcessTaskJacksonModule extends SimpleModule {
    @SuppressWarnings("deprecation")  // For use of ModelManager
    public ProcessTaskJacksonModule(final ModelManager model) {
        this.addSerializer(ProcessTask.class, new ProcessTaskSerializer(model));
        this.addDeserializer(ProcessTask.class, new ProcessTaskDeserializer(model));
    }

    private static class ProcessTaskSerializer extends JsonSerializer<ProcessTask> {
        @SuppressWarnings("deprecation")  // For use of ModelManager
        ProcessTaskSerializer(final ModelManager model) {
            this.model = model;
        }

        @Override
        public void serialize(
                final ProcessTask value,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            final ObjectNode object = OBJECT_MAPPER.createObjectNode();
            object.put("inputType", this.model.writeObjectAsObjectNode(value.getInputPluginType()));
            object.put("outputType", this.model.writeObjectAsObjectNode(value.getOutputPluginType()));
            object.put("filterTypes", this.model.writeObjectAsObjectNode(value.getFilterPluginTypes()));
            object.put("inputTask", this.model.writeObjectAsObjectNode(value.getInputTaskSource()));
            object.put("outputTask", this.model.writeObjectAsObjectNode(value.getOutputTaskSource()));
            object.put("filterTasks", this.model.writeObjectAsObjectNode(value.getFilterTaskSources()));
            object.put("schemas", this.model.writeObjectAsObjectNode(value.getFilterSchemas()));
            object.put("executorSchema", this.model.writeObjectAsObjectNode(value.getExecutorSchema()));
            object.put("executorTask", this.model.writeObjectAsObjectNode(value.getExecutorTaskSource()));
            jsonGenerator.writeTree(object);
        }

        @Deprecated  // https://github.com/embulk/embulk/issues/1304
        private final ModelManager model;
    }

    private static class ProcessTaskDeserializer extends JsonDeserializer<ProcessTask> {
        @SuppressWarnings("deprecation")  // For use of ModelManager
        ProcessTaskDeserializer(final ModelManager model) {
            this.model = model;
        }

        @Override
        public ProcessTask deserialize(
                final JsonParser jsonParser,
                final DeserializationContext context)
                throws JsonMappingException {
            final JsonNode node;
            try {
                node = OBJECT_MAPPER.readTree(jsonParser);
            } catch (final JsonParseException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to parse JSON.", ex);
            } catch (final JsonProcessingException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to process JSON in parsing.", ex);
            } catch (final IOException ex) {
                throw JsonMappingException.from(jsonParser, "Failed to read JSON in parsing.", ex);
            }

            if (!node.isObject()) {
                throw new JsonMappingException("Expected object to deserialize ProcessTask", jsonParser.getCurrentLocation());
            }

            final ObjectNode object = (ObjectNode) node;

            try {
                final PluginType inputPluginType =
                        this.model.readObject(PluginType.class, object.get("inputType").traverse());
                final PluginType outputPluginType =
                        this.model.readObject(PluginType.class, object.get("outputType").traverse());

                final JsonNode filterPluginTypesNode = object.get("filterTypes");
                if (!filterPluginTypesNode.isArray()) {
                    throw new JsonMappingException(
                            "An array is expected for ProcessTask's filterTypes", jsonParser.getCurrentLocation());
                }
                final ArrayList<PluginType> filterPluginTypes = new ArrayList<>();
                for (final JsonNode filterPluginTypeNode : (ArrayNode) filterPluginTypesNode) {
                    if (filterPluginTypeNode == null || filterPluginTypeNode.isNull()) {
                        filterPluginTypes.add(null);
                    } else {
                        filterPluginTypes.add(this.model.readObject(PluginType.class, filterPluginTypeNode.traverse()));
                    }
                }

                final TaskSource inputTaskSource =
                        this.model.readObject(TaskSource.class, object.get("inputTask").traverse());
                final TaskSource outputTaskSource =
                        this.model.readObject(TaskSource.class, object.get("outputTask").traverse());

                final JsonNode filterTaskSourcesNode = object.get("filterTasks");
                if (!filterTaskSourcesNode.isArray()) {
                    throw new JsonMappingException(
                            "An array is expected for ProcessTask's filterTasks", jsonParser.getCurrentLocation());
                }
                final ArrayList<TaskSource> filterTaskSources = new ArrayList<>();
                for (final JsonNode filterTaskSourceNode : (ArrayNode) filterTaskSourcesNode) {
                    if (filterTaskSourceNode == null || filterTaskSourceNode.isNull()) {
                        filterTaskSources.add(null);
                    } else {
                        filterTaskSources.add(this.model.readObject(TaskSource.class, filterTaskSourceNode.traverse()));
                    }
                }

                final JsonNode schemasNode = object.get("schemas");
                if (!schemasNode.isArray()) {
                    throw new JsonMappingException(
                            "An array is expected for ProcessTask's schemas", jsonParser.getCurrentLocation());
                }
                final ArrayList<Schema> schemas = new ArrayList<>();
                for (final JsonNode schemaNode : (ArrayNode) schemasNode) {
                    if (schemaNode == null || schemaNode.isNull()) {
                        schemas.add(null);
                    } else {
                        schemas.add(this.model.readObject(Schema.class, schemaNode.traverse()));
                    }
                }

                final Schema executorSchema =
                        this.model.readObject(Schema.class, object.get("executorSchema").traverse());
                final TaskSource executorTaskSource =
                        this.model.readObject(TaskSource.class, object.get("executorTask").traverse());

                return new ProcessTask(
                        inputPluginType,
                        outputPluginType,
                        Collections.unmodifiableList(filterPluginTypes),
                        inputTaskSource,
                        outputTaskSource,
                        Collections.unmodifiableList(filterTaskSources),
                        Collections.unmodifiableList(schemas),
                        executorSchema,
                        executorTaskSource);
            } catch (final ConfigException ex) {
                throw JsonMappingException.from(jsonParser, "Invalid object to deserialize ProcessTask", ex);
            }
        }

        @Deprecated  // https://github.com/embulk/embulk/issues/1304
        private final ModelManager model;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
