package org.embulk.spi;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public abstract class AbstractReporterImpl
        implements AutoCloseable, Reporter {
    @Override
    public final void reportString(Level level, String string) {
        report(level, ImmutableMap.of("string", (Object)string));
    }

    // TODO reportBuffer, Columns,..

    @Override
    public abstract void report(Level level, Map<String, Object> event);

    @Override
    public abstract void close();

    public abstract void cleanup();
}
