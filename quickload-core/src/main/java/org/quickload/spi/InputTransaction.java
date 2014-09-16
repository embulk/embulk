package org.quickload.spi;

import java.util.List;

public interface InputTransaction
{
    public InputTask getInputTask();

    public void begin();

    public void commit(List<Report> reports);

    public void abort(/* TODO */);
}
