package org.embulk.record;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Type
{
    @JsonValue
    public String getName();

    public Class<?> getJavaType();

    public byte getFixedStorageSize();

    public TypeWriter newWriter(PageBuilder builder, Column column);

    public TypeReader newReader(PageReader cursor, Column column);
}
