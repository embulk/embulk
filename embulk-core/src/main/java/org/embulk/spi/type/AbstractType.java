package org.embulk.spi.type;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

    @SuppressFBWarnings(value = "EQ_UNUSUAL")
    @Override
    public boolean equals(Object o)
    {
        if (o == null) {
            return false;
        }
        return o.getClass().isAssignableFrom(getClass());
    }

    @Override
    public int hashCode()
    {
        return getClass().hashCode();
    }

    @Override
    public String toString()
    {
        return name;
    }
}
