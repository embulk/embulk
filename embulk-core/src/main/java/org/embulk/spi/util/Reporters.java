package org.embulk.spi.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.embulk.spi.Reporter;
import org.embulk.spi.ReporterCloseable;

import java.util.Map;

public final class Reporters
        implements AutoCloseable
{
    public enum Type
    {
        SKIPPED_DATA("skipped_data"),
        EVENT_LOG("event_log");
        // TODO NOTIFICATION

        private final String type;

        Type(final String type)
        {
            this.type = type;
        }

        @JsonCreator
        public static Type fromString(final String type)
        {
            switch (type) {
                case "skipped_data":
                    return SKIPPED_DATA;
                case "event_log":
                    return EVENT_LOG;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @JsonValue
        public String toString()
        {
            return this.type;
        }
    }

    public enum ReportLevel
    {
        DEBUG, INFO, WARN, ERROR, FATAL; // TODO
    }

    private final Map<Reporters.Type, Reporter> reporters;

    Reporters(final Map<Reporters.Type, Reporter> reporters)
    {
        this.reporters = reporters;
    }

    public Reporter getReporter(Reporters.Type type)
    {
        return this.reporters.get(type);
    }

    public void close()
    {
        for (final Map.Entry<Reporters.Type, Reporter> e : reporters.entrySet()) {
            final ReporterCloseable reporter = (ReporterCloseable) e.getValue();
            reporter.close(); // TODO exception?
        }
    }

    public void cleanup()
    {
        for (final Map.Entry<Reporters.Type, Reporter> e : reporters.entrySet()) {
            final ReporterCloseable reporter = (ReporterCloseable) e.getValue();
            reporter.cleanup(); // TODO exception?
        }
    }
}
