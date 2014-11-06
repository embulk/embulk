package org.quickload.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.yaml.snakeyaml.Yaml;

public class ConfigSources
{
    public static ConfigSource fromJson(JsonParser parser) throws IOException
    {
        JsonNode json = new ObjectMapper().readTree(parser);
        if (!json.isObject()) {
            throw new JsonMappingException("Expected object to deserialize ConfigSource but got "+json);
        }
        return fromJson((ObjectNode) json);
    }

    public static ConfigSource fromJson(ObjectNode data)
    {
        return new ConfigSource(data.deepCopy());
    }

    public static ConfigSource fromYamlFile(File path) throws IOException
    {
        Yaml yaml = new Yaml();
        Object parsedYaml;
        try (FileInputStream is = new FileInputStream(path)) {
            parsedYaml = yaml.load(is);
        }
        ObjectNode source = objectToJsonObject(parsedYaml);
        return new ConfigSource(source);
    }

    public static ConfigSource fromPropertiesYamlLiteral(Properties props, String keyPrefix)
    {
        // TODO exception handling
        ObjectNode source = new ObjectNode(JsonNodeFactory.instance);
        Yaml yaml = new Yaml();
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith(keyPrefix)) {
                continue;
            }
            String yamlValue = props.getProperty(key);
            String keyName = key.substring(keyPrefix.length());
            Object parsedValue = yaml.load(yamlValue);
            JsonNode typedValue = objectToJson(parsedValue);
            source.set(keyName, typedValue);
        }
        return new ConfigSource(source);
    }

    private static JsonNode objectToJson(Object object)
    {
        // TODO exception
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(object));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static ObjectNode objectToJsonObject(Object object)
    {
        // TODO exception
        JsonNode json = objectToJson(object);
        if (!json.isObject()) {
            throw new RuntimeException("Expected object to deserialize ConfigSource but got "+json);
        }
        return (ObjectNode) json;
    }
}
