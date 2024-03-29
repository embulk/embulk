package org.embulk.deps.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSource;
import org.embulk.config.ModelManagerDelegate;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelManagerDelegateImpl extends ModelManagerDelegate {
    private final Logger logger = LoggerFactory.getLogger(ModelManagerDelegateImpl.class);

    private final ObjectMapper objectMapper;
    private final ObjectMapper configObjectMapper;  // configObjectMapper uses different TaskDeserializer

    public ModelManagerDelegateImpl() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ColumnConfigJacksonModule(this));
        objectMapper.registerModule(new SchemaConfigJacksonModule(this));
        objectMapper.registerModule(new PluginTypeJacksonModule());
        objectMapper.registerModule(new ProcessTaskJacksonModule(this));
        objectMapper.registerModule(new ResumeStateJacksonModule(this));
        objectMapper.registerModule(new TimestampJacksonModule());  // Deprecated. TBD to remove or not.
        objectMapper.registerModule(new TimestampFormatJacksonModule());
        objectMapper.registerModule(new ByteSizeJacksonModule());
        objectMapper.registerModule(new CharsetJacksonModule());
        objectMapper.registerModule(new LocalFileJacksonModule());
        objectMapper.registerModule(new ToStringJacksonModule());
        objectMapper.registerModule(new ToStringMapJacksonModule());
        objectMapper.registerModule(new TypeJacksonModule());
        objectMapper.registerModule(new ColumnJacksonModule());
        objectMapper.registerModule(new SchemaJacksonModule());
        objectMapper.registerModule(new Jdk8Module());  // jackson-datatype-jdk8
        this.configObjectMapper = objectMapper.copy();

        objectMapper.registerModule(new TaskSerDe.TaskSerializerModule(objectMapper));
        objectMapper.registerModule(new TaskSerDe.TaskDeserializerModule(objectMapper, this));
        objectMapper.registerModule(new DataSourceSerDe.SerDeModule(this));
        configObjectMapper.registerModule(new TaskSerDe.TaskSerializerModule(configObjectMapper));
        configObjectMapper.registerModule(new TaskSerDe.ConfigTaskDeserializerModule(configObjectMapper, this));
        configObjectMapper.registerModule(new DataSourceSerDe.SerDeModule(this));
    }

    @Override
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

    <T> T readObject(Class<T> valueType, JsonParser parser) {
        try {
            return objectMapper.readValue(parser, valueType);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    @Override
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
        return t;
    }

    <T> T readObjectWithConfigSerDe(Class<T> valueType, JsonParser parser) {
        T t;
        try {
            t = configObjectMapper.readValue(parser, valueType);
        } catch (Exception ex) {
            if (ex instanceof ConfigException) {
                throw (ConfigException) ex;
            }
            throw new ConfigException(ex);
        }
        return t;
    }

    @Override
    public DataSource readObjectAsDataSource(final String json) {
        try {
            return objectMapper.readValue(json, DataSourceImpl.class);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
        }
    }

    @Override
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

    @Override
    public void validate(Object object) {
        logger.warn(
                "ModelManager#validate is no longer available.",
                new UnsupportedOperationException("ModelManager#validate is no longer available."));
    }

    @Override
    public TaskReport newTaskReport() {
        return new DataSourceImpl(this);
    }

    @Override
    public ConfigDiff newConfigDiff() {
        return new DataSourceImpl(this);
    }

    @Override
    public ConfigSource newConfigSource() {
        return new DataSourceImpl(this);
    }

    @Override
    public TaskSource newTaskSource() {
        return new DataSourceImpl(this);
    }

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
