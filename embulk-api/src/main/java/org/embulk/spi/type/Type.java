package org.embulk.spi.type;

public interface Type {
    String getName();

    Class<?> getJavaType();

    byte getFixedStorageSize();
}
