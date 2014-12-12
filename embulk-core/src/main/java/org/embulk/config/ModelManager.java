package org.embulk.config;

import javax.validation.Validation;
import org.apache.bval.jsr303.ApacheValidationProvider;
import com.google.inject.Inject;
import com.google.common.base.Throwables;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ModelManager
{
    private final ObjectMapper objectMapper;
    private final ObjectMapper configObjectMapper;  // configObjectMapper uses different TaskDeserializer
    private final TaskValidator taskValidator;

    @Inject
    public ModelManager(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
        this.configObjectMapper = objectMapper.copy();
        this.taskValidator = new TaskValidator(
                Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory().getValidator());

        objectMapper.registerModule(new TaskSerDe.TaskSerializerModule(objectMapper));
        objectMapper.registerModule(new TaskSerDe.TaskDeserializerModule(objectMapper, taskValidator));
        configObjectMapper.registerModule(new TaskSerDe.TaskSerializerModule(configObjectMapper));
        configObjectMapper.registerModule(new TaskSerDe.ConfigTaskDeserializerModule(configObjectMapper, taskValidator));
    }

    // TODO inject by Set<Module> because this is not thread-safe?
    public void registerObjectMapperModule(Module module)
    {
        objectMapper.registerModule(module);
        configObjectMapper.registerModule(module);
    }

    public <T> T readObject(DataSource<?> json, Class<T> valueType)
    {
        return readObject(json.getSource().traverse(), valueType);
    }

    public <T> T readObject(JsonParser json, Class<T> valueType)
    {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public <T extends Task> T readTaskConfig(DataSource<?> json, Class<T> taskType)
    {
        return readTaskConfig(json.getSource().traverse(), taskType);
    }

    public <T extends Task> T readTaskConfig(JsonParser json, Class<T> taskType)
    {
        T t;
        try {
            t = configObjectMapper.readValue(json, taskType);
        } catch (Exception ex) {
            throw new ConfigException(ex);
        }
        t.validate();
        return t;
    }

    public String writeAsJsonString(Object object)
    {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public TaskSource writeAsTaskSource(Object object)
    {
        String json = writeAsJsonString(object);
        try {
            return new TaskSource((ObjectNode) objectMapper.readTree(json));
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public ConfigSource writeAsConfigSource(Object object)
    {
        String json = writeAsJsonString(object);
        try {
            return new ConfigSource((ObjectNode) objectMapper.readTree(json));
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }
}
