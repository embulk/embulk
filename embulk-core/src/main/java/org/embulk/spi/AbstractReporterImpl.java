package org.embulk.spi;

import com.google.common.collect.ImmutableMap;
import org.embulk.spi.Reporter;

import java.util.Map;

public abstract class AbstractReporterImpl
        implements AutoCloseable, Reporter
{
    public final void reportLine(Level level, String line)
    {
        report(level, ImmutableMap.of("skipped_line", line));
    }

    // TODO reportBuffer, Columns,..

    public abstract void report(Level level, Map<String, Object> event);

    public abstract void close();

    public abstract void cleanup();
}
