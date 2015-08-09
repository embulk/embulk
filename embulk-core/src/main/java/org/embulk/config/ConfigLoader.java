package org.embulk.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.yaml.snakeyaml.Yaml;

public class ConfigLoader
{
    private final ModelManager model;

    @Inject
    public ConfigLoader(ModelManager model)
    {
        this.model = model;
    }

    public ConfigSource newConfigSource()
    {
        return new DataSourceImpl(model);
    }

    public ConfigSource fromJsonFile(File file) throws IOException
    {
        try (FileInputStream is = new FileInputStream(file)) {
            return fromJson(is);
        }
    }

    public ConfigSource fromJson(InputStream stream) throws IOException
    {
        JsonNode node = new ObjectMapper().readTree(stream);
        if (!node.isObject()) {
            throw new RuntimeJsonMappingException("Expected object to load ConfigSource but got: "+node.getNodeType());
        }
        return new DataSourceImpl(model, (ObjectNode) node);
    }

    public ConfigSource fromYamlFile(File file) throws IOException
    {
        try (FileInputStream stream = new FileInputStream(file)) {
            return fromYaml(stream);
        }
    }

    public ConfigSource fromYaml(InputStream stream) throws IOException
    {
        Yaml yaml = new Yaml();
        Object object = yaml.load(stream);
        JsonNode node = objectToJson(object);
        if (!node.isObject()) {
            throw new RuntimeJsonMappingException("Expected object to load ConfigSource but got "+node);
        }
        return new DataSourceImpl(model, (ObjectNode) node);
    }

    @Deprecated
    public ConfigSource fromJson(JsonParser parser) throws IOException
    {
        // TODO check parsed.isObject()
        ObjectNode source = (ObjectNode) new ObjectMapper().readTree(parser);
        return new DataSourceImpl(model, source);
    }

    public ConfigSource fromPropertiesYamlLiteral(Properties props, String keyPrefix)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (String propName : props.stringPropertyNames()) {
            builder.put(propName, props.getProperty(propName));
        }
        return fromPropertiesYamlLiteral(builder.build(), keyPrefix);
    }

    public ConfigSource fromPropertiesYamlLiteral(Map<String, String> props, String keyPrefix)
    {
        ObjectNode source = new ObjectNode(JsonNodeFactory.instance);
        DataSource ds = new DataSourceImpl(model, source);
        Yaml yaml = new Yaml();
        for (Map.Entry<String, String> pair : props.entrySet()) {
            if (!pair.getKey().startsWith(keyPrefix)) {
                continue;
            }
            String keyName = pair.getKey().substring(keyPrefix.length());
            Object parsedValue = yaml.load(pair.getValue());  // TODO exception handling
            JsonNode node = objectToJson(parsedValue);

            // handle "." as a map acccessor. for example:
            // in.parser.type=csv => {"in": {"parser": {"type": "csv"}}}
            // TODO handle "[]" as array index
            String[] fragments = keyName.split("\\.");
            DataSource key = ds;
            for (int i=0; i < fragments.length - 1; i++) {
                key = key.getNestedOrSetEmpty(fragments[i]);  // TODO exception handling
            }
            key.set(fragments[fragments.length - 1], node);
        }
        return new DataSourceImpl(model, source);
    }

    private JsonNode objectToJson(Object object)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(object));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
