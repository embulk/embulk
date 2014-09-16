package org.quickload.config;

public interface DynamicModel <T extends DynamicModel>
{
    public T validate();

    public Object get(String attrName);

    public T set(String attrName, Object value);
}
