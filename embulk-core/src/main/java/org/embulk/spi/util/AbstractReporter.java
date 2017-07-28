package org.embulk.spi.util;

import com.google.common.collect.ImmutableMap;
import org.embulk.spi.ReporterCloseable;

import java.util.Map;

public abstract class AbstractReporter
        implements ReporterCloseable
{
    public final void reportLine(Reporters.ReportLevel level, String line)
    {
        report(level, ImmutableMap.of("skipped_line", line));
    }

    // TODO reportBuffer, Columns,..

    public abstract void report(Reporters.ReportLevel level, Map<String, Object> event);
}
