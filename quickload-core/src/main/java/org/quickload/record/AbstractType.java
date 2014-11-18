package org.quickload.record;

public abstract class AbstractType
        implements Type
{
    private final String name;
    private final Class<?> javaType;
    private byte fixedStorageSize;

    protected AbstractType(String name, Class<?> javaType, int fixedStorageSize)
    {
        this.name = name;
        this.javaType = javaType;
        this.fixedStorageSize = (byte) fixedStorageSize;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Class<?> getJavaType()
    {
        return javaType;
    }

    @Override
    public byte getFixedStorageSize()
    {
        return fixedStorageSize;
    }
}
