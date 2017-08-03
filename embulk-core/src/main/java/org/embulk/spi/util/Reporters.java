package org.embulk.spi.util;

import org.embulk.spi.Reporter;
import org.embulk.spi.ReporterCloseable;

import java.util.Map;

public final class Reporters
        implements AutoCloseable
{

    private final Map<Reporter.Channel, Reporter> reporters;

    Reporters(final Map<Reporter.Channel, Reporter> reporters)
    {
        this.reporters = reporters;
    }

    public Reporter getReporter(Reporter.Channel channel)
    {
        return this.reporters.get(channel);
    }

    public void close()
    {
        for (final Map.Entry<Reporter.Channel, Reporter> e : reporters.entrySet()) {
            final ReporterCloseable reporter = (ReporterCloseable) e.getValue();
            reporter.close(); // TODO exception?
        }
    }

    public void cleanup()
    {
        for (final Map.Entry<Reporter.Channel, Reporter> e : reporters.entrySet()) {
            final ReporterCloseable reporter = (ReporterCloseable) e.getValue();
            reporter.cleanup(); // TODO exception?
        }
    }
}
