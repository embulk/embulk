package org.embulk.spi;

public interface Mixin <T>
{
    public T mixin(T target);
}
