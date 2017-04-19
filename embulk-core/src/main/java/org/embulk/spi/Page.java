package org.embulk.spi;

import java.util.List;
import org.msgpack.value.ImmutableValue;

public class Page
{
    private final Buffer buffer;
    private List<byte[]> binaryReferences;
    private List<String> stringReferences;
    private List<ImmutableValue> valueReferences;

    protected Page(Buffer buffer)
    {
        this.buffer = buffer;
    }

    public static Page allocate(int length)
    {
        return new Page(Buffer.allocate(length));
    }

    public static Page wrap(Buffer buffer)
    {
        return new Page(buffer);
    }

    public Page setBinaryReferences(List<byte[]> binaryReferences)
    {
        this.binaryReferences = binaryReferences;
        return this;
    }

    public Page setStringReferences(List<String> values)
    {
        this.stringReferences = values;
        return this;
    }

    public Page setValueReferences(List<ImmutableValue> values)
    {
        this.valueReferences = values;
        return this;
    }

    public List<byte[]> getBinaryReferences()
    {
        // TODO used by mapreduce executor
        return binaryReferences;
    }

    public List<String> getStringReferences()
    {
        // TODO used by mapreduce executor
        return stringReferences;
    }

    public List<ImmutableValue> getValueReferences()
    {
        // TODO used by mapreduce executor
        return valueReferences;
    }

    public byte[] getBinaryReference(int index)
    {
        return binaryReferences.get(index);
    }

    public String getStringReference(int index)
    {
        return stringReferences.get(index);
    }

    public ImmutableValue getValueReference(int index)
    {
        return valueReferences.get(index);
    }

    public void release()
    {
        buffer.release();
    }

    public Buffer buffer()
    {
        return buffer;
    }
}
