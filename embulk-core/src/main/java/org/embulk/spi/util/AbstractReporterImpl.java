package org.embulk.spi.util;

import com.google.common.collect.ImmutableMap;
import org.embulk.spi.ReporterCloseable;

import java.util.Map;

public abstract class AbstractReporterImpl
        implements ReporterCloseable
{
    public final void reportLine(Level level, String line)
    {
        report(level, ImmutableMap.of("skipped_line", line));
    }

    // TODO reportBuffer, Columns,..

    public abstract void report(Level level, Map<String, Object> event);
}
