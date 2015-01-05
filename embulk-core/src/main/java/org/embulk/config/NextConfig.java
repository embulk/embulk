package org.embulk.config;

public interface NextConfig
        extends DataSource
{
    @Override
    public NextConfig getNested(String attrName);

    @Override
    public NextConfig getNestedOrSetEmpty(String attrName);

    @Override
    public NextConfig set(String attrName, Object v);

    @Override
    public NextConfig setNested(String attrName, DataSource v);

    @Override
    public NextConfig setAll(DataSource other);

    @Override
    public NextConfig deepCopy();

    @Override
    public NextConfig merge(DataSource other);
}
