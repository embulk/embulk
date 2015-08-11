package org.embulk.spi.type;

public interface TypeSinkCaller <T>
{
    public void call(T context);
}
