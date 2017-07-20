package org.embulk.spi;

import org.embulk.spi.util.Reporters;

import java.util.Map;

public interface Reporter
{
    void report(Reporters.ReportLevel level, Map<String, Object> event);
}
