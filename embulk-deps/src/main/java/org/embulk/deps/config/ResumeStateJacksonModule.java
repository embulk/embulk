package org.embulk.deps.config;

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
import java.util.Optional;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.ResumeState;
import org.embulk.spi.Schema;

final class ResumeStateJacksonModule extends SimpleModule {
    public ResumeStateJacksonModule(final ModelManagerDelegateImpl model) {
        this.addSerializer(ResumeState.class, new ResumeStateSerializer(model));
        this.addDeserializer(ResumeState.class, new ResumeStateDeserializer(model));
    }

    private static class ResumeStateSerializer extends JsonSerializer<ResumeState> {
        ResumeStateSerializer(final ModelManagerDelegateImpl model) {
            this.model = model;
        }

        @Override
        public void serialize(
                final ResumeState value,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            final ObjectNode object = OBJECT_MAPPER.createObjectNode();
            object.put("exec_task", this.model.writeObjectAsObjectNode(value.getExecSessionConfigSource()));
            object.put("in_task", this.model.writeObjectAsObjectNode(value.getInputTaskSource()));
            object.put("out_task", this.model.writeObjectAsObjectNode(value.getOutputTaskSource()));
            object.put("in_schema", this.model.writeObjectAsObjectNode(value.getInputSchema()));
            object.put("out_schema", this.model.writeObjectAsObjectNode(value.getOutputSchema()));
            object.put("in_reports", this.model.writeObjectAsObjectNode(value.getInputTaskReports()));
            object.put("out_reports", this.model.writeObjectAsObjectNode(value.getOutputTaskReports()));
            jsonGenerator.writeTree(object);
        }

        private final ModelManagerDelegateImpl model;
    }

    private static class ResumeStateDeserializer extends JsonDeserializer<ResumeState> {
        ResumeStateDeserializer(final ModelManagerDelegateImpl model) {
            this.model = model;
        }

        @Override
        public ResumeState deserialize(
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
                throw new JsonMappingException("Expected object to deserialize ResumeState", jsonParser.getCurrentLocation());
            }

            final ObjectNode object = (ObjectNode) node;

            try {
                final ConfigSource execSessionConfigSource =
                        this.model.readObject(ConfigSource.class, object.get("exec_task").traverse());
                final TaskSource inputTaskSource =
                        this.model.readObject(TaskSource.class, object.get("in_task").traverse());
                final TaskSource outputTaskSource =
                        this.model.readObject(TaskSource.class, object.get("out_task").traverse());
                final Schema inputSchema =
                        this.model.readObject(Schema.class, object.get("in_schema").traverse());
                final Schema outputSchema =
                        this.model.readObject(Schema.class, object.get("out_schema").traverse());

                final JsonNode inputTaskReportsNode = object.get("in_reports");
                if (!inputTaskReportsNode.isArray()) {
                    throw new JsonMappingException(
                            "An array is expected for ResumeState's in_reports", jsonParser.getCurrentLocation());
                }
                final ArrayList<Optional<TaskReport>> inputTaskReports = new ArrayList<>();
                for (final JsonNode inputTaskReportNode : (ArrayNode) inputTaskReportsNode) {
                    if (inputTaskReportNode == null || inputTaskReportNode.isNull()) {
                        inputTaskReports.add(Optional.<TaskReport>empty());
                    } else {
                        inputTaskReports.add(Optional.of(this.model.readObject(TaskReport.class, inputTaskReportNode.traverse())));
                    }
                }

                final JsonNode outputTaskReportsNode = object.get("out_reports");
                if (!outputTaskReportsNode.isArray()) {
                    throw new JsonMappingException(
                            "An array is expected for ResumeState's out_reports", jsonParser.getCurrentLocation());
                }
                final ArrayList<Optional<TaskReport>> outputTaskReports = new ArrayList<>();
                for (final JsonNode outputTaskReportNode : (ArrayNode) outputTaskReportsNode) {
                    if (outputTaskReportNode == null || outputTaskReportNode.isNull()) {
                        outputTaskReports.add(Optional.<TaskReport>empty());
                    } else {
                        outputTaskReports.add(Optional.of(this.model.readObject(TaskReport.class, outputTaskReportNode.traverse())));
                    }
                }

                return new ResumeState(
                        execSessionConfigSource,
                        inputTaskSource,
                        outputTaskSource,
                        inputSchema,
                        outputSchema,
                        Collections.unmodifiableList(inputTaskReports),
                        Collections.unmodifiableList(outputTaskReports));
            } catch (final ConfigException ex) {
                throw JsonMappingException.from(jsonParser, "Invalid object to deserialize ResumeState", ex);
            }
        }

        private final ModelManagerDelegateImpl model;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
