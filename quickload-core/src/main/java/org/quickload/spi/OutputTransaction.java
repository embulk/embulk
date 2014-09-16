package org.quickload.spi;

import java.util.List;

public interface OutputTransaction
{
    public OutputTask getOutputTask(InputTask input);

    public void begin();

    public void commit(List<Report> reports);

    public void abort(/* TODO */);
}
