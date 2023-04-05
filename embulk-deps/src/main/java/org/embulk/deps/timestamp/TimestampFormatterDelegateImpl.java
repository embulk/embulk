package org.embulk.deps.timestamp;

import java.time.Instant;
import org.embulk.spi.time.TimestampFormatterDelegate;
import org.embulk.util.timestamp.TimestampFormatter;

public final class TimestampFormatterDelegateImpl extends TimestampFormatterDelegate {
    public TimestampFormatterDelegateImpl(final String pattern, final String defaultZone, final String defaultDate) {
        final TimestampFormatter.Builder builder = TimestampFormatter.builder(pattern, true);
        if (defaultZone != null) {
            builder.setDefaultZoneFromString(defaultZone);
        }
        if (defaultDate != null) {
            builder.setDefaultDateFromString(defaultDate);
        }
        this.formatter = builder.build();
    }

    @Override
    public String format(final Instant format) {
        return this.formatter.format(format);
    }

    @Override
    public Instant parse(final String text) {
        return this.formatter.parse(text);
    }

    private final TimestampFormatter formatter;
}
