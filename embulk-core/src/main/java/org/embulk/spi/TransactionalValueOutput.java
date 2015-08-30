package org.embulk.spi;

import org.msgpack.value.Value;
import org.embulk.config.TaskReport;

public interface TransactionalValueOutput
        extends Transactional, ValueOutput
{
    void add(Value value);

    void abort();

    TaskReport commit();

    void close();
}
