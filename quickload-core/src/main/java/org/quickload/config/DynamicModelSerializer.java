package org.quickload.config;

import java.util.Map;
import java.util.Set;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quickload.config.DynamicModeler.InstanceFactory;

public class DynamicModelSerializer <T extends DynamicModel<T>>
        extends JsonSerializer<T>
{
    private final Set<String> fieldNames;
    private final ObjectMapper nestedObjectMapper;

    public DynamicModelSerializer(Set<String> fieldNames,
            ObjectMapper nestedObjectMapper)
    {
        this.fieldNames = fieldNames;
        this.nestedObjectMapper = nestedObjectMapper;
    }

    @Override
    public void serialize(T instance, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
    {
        // TODO
    }
}
