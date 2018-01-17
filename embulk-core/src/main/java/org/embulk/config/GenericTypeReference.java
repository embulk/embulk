package org.embulk.config;

import com.fasterxml.jackson.core.type.TypeReference;
import java.lang.reflect.Type;

class GenericTypeReference extends TypeReference<Object> {
    private final Type type;

    public GenericTypeReference(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
