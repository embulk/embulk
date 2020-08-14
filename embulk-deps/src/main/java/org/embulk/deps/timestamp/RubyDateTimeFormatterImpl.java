package org.embulk.deps.timestamp;

import java.time.temporal.TemporalAccessor;
import org.embulk.util.rubytime.RubyDateTimeFormatter;

public final class RubyDateTimeFormatterImpl extends org.embulk.deps.timestamp.RubyDateTimeFormatter {
    public RubyDateTimeFormatterImpl(final String pattern) {
        this.formatter = RubyDateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public String format(final TemporalAccessor temporal) {
        return this.formatter.formatWithZoneNameStyle(temporal, RubyDateTimeFormatter.ZoneNameStyle.SHORT);
    }

    private final RubyDateTimeFormatter formatter;
}
