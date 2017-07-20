package org.embulk.spi;

public interface Reporter
        extends AutoCloseable
{
    public enum ReportType
    {
        SKIP_RECORD("skip_record"), EVENT_LOG("event_log"); // TODO

        private final String type;

        ReportType(final String type)
        {
            this.type = type;
        }

        public String getType()
        {
            return this.type;
        }
    }

    public enum ReportLevel
    {
        DEBUG, INFO, WARN, ERROR, FATAL; // TODO
    }

    void close(); // TODO should consider about the return type

    void cleanup(); // TODO should return TaskReport??
}
