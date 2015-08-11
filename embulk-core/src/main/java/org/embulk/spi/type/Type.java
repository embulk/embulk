package org.embulk.spi.type;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageReader;

@JsonDeserialize(using=TypeDeserializer.class)
public interface Type
{
    @JsonValue
    public String getName();

    public Class<?> getJavaType();

    public byte getFixedStorageSize();

    public <C> TypeSinkCaller<C> newTypeSinkCaller(final ValueProducer<C> producer, PageBuilder pageBuilder, Column column);

    public TypeSource newSource(PageReader pageReader, Column column);
}
