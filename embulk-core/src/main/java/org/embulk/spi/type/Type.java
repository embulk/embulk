package org.embulk.spi.type;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using=TypeDeserializer.class)
public interface Type
{
    @JsonValue
    String getName();

    Class<?> getJavaType();

    byte getFixedStorageSize();
}
