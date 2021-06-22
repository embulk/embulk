package org.embulk.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import javax.validation.Validation;
import org.apache.bval.jsr303.ApacheValidationProvider;

@Deprecated  // https://github.com/embulk/embulk/issues/1304
public class ModelManager {
    private final ObjectMapper objectMapper;
    private final ObjectMapper configObjectMapper;  // configObjectMapper uses different TaskDeserializer
    private final TaskValidator taskValidator;

    public ModelManager() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new PluginTypeJacksonModule());
        objectMapper.registerModule(new TimestampJacksonModule());  // Deprecated. TBD to remove or not.
        objectMapper.registerModule(new CharsetJacksonModule());
        objectMapper.registerModule(new LocalFileJacksonModule());
        objectMapper.registerModule(new ToStringJacksonModule());
        objectMapper.registerModule(new ToStringMapJacksonModule());
        objectMapper.registerModule(new TypeJacksonModule());
        objectMapper.registerModule(new ColumnJacksonModule());
        objectMapper.registerModule(new SchemaJacksonModule());
        objectMapper.registerModule(new GuavaModule());  // jackson-datatype-guava
        objectMapper.registerModule(new Jdk8Module());  // jackson-datatype-jdk8
        this.configObjectMapper = objectMapper.copy();
        this.taskValidator = new TaskValidator(
                Validation.byProvider(ApacheValidationProvider.class).configure().buildValidatorFactory().getValidator());

        objectMapper.registerModule(new TaskSerDe.TaskSerializerModule(objectMapper));
        objectMapper.registerModule(new TaskSerDe.TaskDeserializerModule(objectMapper, this));
        objectMapper.registerModule(new DataSourceSerDe.SerDeModule(this));
        configObjectMapper.registerModule(new TaskSerDe.TaskSerializerModule(configObjectMapper));
        configObjectMapper.registerModule(new TaskSerDe.ConfigTaskDeserializerModule(configObjectMapper, this));
        configObjectMapper.registerModule(new DataSourceSerDe.SerDeModule(this));
    }

    public <T> T readObject(Class<T> valueType, String json) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    public <T> T readObject(Class<T> valueType, JsonParser parser) {
        try {
            return objectMapper.readValue(parser, valueType);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    public <T> T readObjectWithConfigSerDe(Class<T> valueType, String json) {
        T t;
        try {
            t = configObjectMapper.readValue(json, valueType);
        } catch (Exception ex) {
            if (ex instanceof ConfigException) {
                throw (ConfigException) ex;
            }
            throw new ConfigException(ex);
        }
        validate(t);
        return t;
    }

    public <T> T readObjectWithConfigSerDe(Class<T> valueType, JsonParser parser) {
        T t;
        try {
            t = configObjectMapper.readValue(parser, valueType);
        } catch (Exception ex) {
            if (ex instanceof ConfigException) {
                throw (ConfigException) ex;
            }
            throw new ConfigException(ex);
        }
        validate(t);
        return t;
    }

    public String writeObject(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    public void validate(Object object) {
        taskValidator.validateModel(object);
    }

    // visible for DataSource.set
    JsonNode writeObjectAsJsonNode(Object v) {
        String json = writeObject(v);
        try {
            return objectMapper.readValue(json, JsonNode.class);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    // visible for TaskInvocationHandler.invokeDump
    ObjectNode writeObjectAsObjectNode(Object v) {
        String json = writeObject(v);
        try {
            return objectMapper.readValue(json, ObjectNode.class);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }
}
