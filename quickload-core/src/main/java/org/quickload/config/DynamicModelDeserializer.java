package org.quickload.config;

import java.util.Map;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quickload.config.DynamicModeler.InstanceFactory;

public class DynamicModelDeserializer <T extends DynamicModel<T>>
        extends JsonDeserializer<T>
{
    private final InstanceFactory<T> factory;
    private final Map<String, TypeReference<?>> fields;
    private final ObjectMapper nestedObjectMapper;

    public DynamicModelDeserializer(InstanceFactory<T> factory,
            Map<String, TypeReference<?>> fields,
            ObjectMapper nestedObjectMapper)
    {
        this.factory = factory;
        this.fields = fields;
        this.nestedObjectMapper = nestedObjectMapper;
    }

    @Override
    public T deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        if (jp.nextToken() != JsonToken.START_OBJECT) {
            throw new RuntimeJsonMappingException("Expected object to deserialize config object");
        }

        T instance = factory.newInstance();

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            TypeReference<?> fieldType = fields.get(fieldName);
            if (fieldType != null) {
                Object value = nestedObjectMapper.readValue(jp, fieldType);  // TODO ctx.getConfig?
                instance.set(fieldName, value);
            } else {
                jp.skipChildren();
            }
        }

        return instance;
    }
}
