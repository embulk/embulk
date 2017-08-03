package org.embulk.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

public interface Reporter
{
    void reportString(Level level, String string);

    void report(Level level, Map<String, Object> event);

    enum Channel
    {
        SKIPPED_DATA("skipped_data"),
        LOG("log");
        // TODO NOTIFICATION

        private final String channel;

        Channel(final String channel)
        {
            this.channel = channel;
        }

        @JsonCreator
        public static Channel fromString(final String channel)
        {
            switch (channel) {
                case "skipped_data":
                    return SKIPPED_DATA;
                case "log":
                    return LOG;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @JsonValue
        public String toString()
        {
            return this.channel;
        }
    }

    enum Level
    {
        DEBUG, INFO, WARN, ERROR, FATAL; // TODO
    }
}
