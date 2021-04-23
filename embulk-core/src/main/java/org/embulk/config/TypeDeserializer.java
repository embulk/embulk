package org.embulk.config;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;

class TypeDeserializer extends FromStringDeserializer<Type> {
    private static final Map<String, Type> stringToTypeMap;

    static {
        ImmutableMap.Builder<String, Type> builder = ImmutableMap.builder();
        builder.put(Types.BOOLEAN.getName(), Types.BOOLEAN);
        builder.put(Types.LONG.getName(), Types.LONG);
        builder.put(Types.DOUBLE.getName(), Types.DOUBLE);
        builder.put(Types.STRING.getName(), Types.STRING);
        builder.put(Types.TIMESTAMP.getName(), Types.TIMESTAMP);
        builder.put(Types.JSON.getName(), Types.JSON);
        stringToTypeMap = builder.build();
    }

    public TypeDeserializer() {
        super(Type.class);
    }

    @Override
    protected Type _deserialize(String value, DeserializationContext context) throws IOException {
        Type t = stringToTypeMap.get(value);
        if (t == null) {
            throw new JsonMappingException(
                    String.format("Unknown type name '%s'. Supported types are: %s",
                                  value,
                                  Joiner.on(", ").join(stringToTypeMap.keySet())));
        }
        return t;
    }
}
