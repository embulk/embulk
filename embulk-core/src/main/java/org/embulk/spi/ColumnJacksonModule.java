package org.embulk.spi;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;

public final class ColumnJacksonModule extends SimpleModule {
    public ColumnJacksonModule() {
        this.addSerializer(Column.class, new ColumnSerializer());
        this.addDeserializer(Column.class, new ColumnDeserializer());
    }

    private static class ColumnSerializer extends JsonSerializer<Column> {
        @Override
        public void serialize(
                final Column value,
                final JsonGenerator jsonGenerator,
                final SerializerProvider provider)
                throws IOException {
            final ObjectNode object = OBJECT_MAPPER.createObjectNode();
            object.put("index", value.getIndex());
            object.put("name", value.getName());
            object.put("type", value.getType().getName());
            jsonGenerator.writeTree(object);
        }
    }

    private static class ColumnDeserializer extends StdNodeBasedDeserializer<Column> {
        protected ColumnDeserializer() {
            super(Column.class);
        }

        @Override
        public Column convert(
                final JsonNode root,
                final DeserializationContext context)
                throws JsonProcessingException {
            if (root == null || !root.isObject()) {
                throw JsonMappingException.from(context.getParser(), "Column expects a JSON Object node.");
            }
            final ObjectNode object = (ObjectNode) root;

            final JsonNode indexNode = object.get("index");
            if (indexNode == null) {
                throw JsonMappingException.from(context.getParser(), "index is null.");
            }
            final int index = OBJECT_MAPPER.treeToValue(indexNode, int.class);

            final JsonNode nameNode = object.get("name");
            if (nameNode == null) {
                throw JsonMappingException.from(context.getParser(), "name is null.");
            }
            final String name = OBJECT_MAPPER.treeToValue(nameNode, String.class);

            final JsonNode typeNode = object.get("type");
            if (typeNode == null) {
                throw JsonMappingException.from(context.getParser(), "type is null.");
            }
            final String typeString = OBJECT_MAPPER.treeToValue(typeNode, String.class);

            if (!STRING_TO_TYPE.containsKey(typeString)) {
                throw JsonMappingException.from(context.getParser(), "Unexpected type: " + typeString);
            }
            final Type type = STRING_TO_TYPE.get(typeString);

            return new Column(index, name, type);
        }
    }

    static {
        final HashMap<String, Type> builder = new HashMap<>();
        builder.put(Types.BOOLEAN.getName(), Types.BOOLEAN);
        builder.put(Types.LONG.getName(), Types.LONG);
        builder.put(Types.DOUBLE.getName(), Types.DOUBLE);
        builder.put(Types.STRING.getName(), Types.STRING);
        builder.put(Types.TIMESTAMP.getName(), Types.TIMESTAMP);
        builder.put(Types.JSON.getName(), Types.JSON);
        STRING_TO_TYPE = Collections.unmodifiableMap(builder);
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<String, Type> STRING_TO_TYPE;
}
