package org.embulk.type;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using=TypeDeserializer.class)
public interface Type
{
    @JsonValue
    public String getName();

    public Class<?> getJavaType();

    public byte getFixedStorageSize();
}
