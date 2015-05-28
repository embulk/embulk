package org.embulk.spi.type;

public interface TypeSource
{
    public void getTo(ValueConsumer consumer);
}
