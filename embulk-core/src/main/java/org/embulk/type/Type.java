package org.embulk.type;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Type
{
    @JsonValue
    public String getName();

    public Class<?> getJavaType();

    public byte getFixedStorageSize();
}
