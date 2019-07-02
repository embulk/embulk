package org.embulk.deps.timestamp;

public class RubyDateTimeZonesImpl extends org.embulk.deps.timestamp.RubyDateTimeZones {
    private RubyDateTimeZonesImpl() {
        // No instantiation.
    }

    public static int toOffsetInSeconds(final String zoneName) {
        return org.embulk.util.rubytime.RubyDateTimeZones.toOffsetInSeconds(zoneName);
    }
}
