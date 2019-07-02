package org.embulk.deps.timestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalInt;
import org.embulk.util.timestamp.TimestampFormatter;

public class TimestampFormatterImpl extends org.embulk.deps.timestamp.TimestampFormatter {
    public TimestampFormatterImpl(
            final String pattern,
            final Optional<ZoneOffset> defaultZoneOffset,
            final Optional<ZoneId> defaultZoneId,
            final OptionalInt defaultYear,
            final OptionalInt defaultMonthOfYear,
            final OptionalInt defaultDayOfMonth) {
        final TimestampFormatter.Builder builder = TimestampFormatter.builder(pattern, true);
        if (pattern.startsWith("ruby:") || pattern.startsWith("java:")) {
            if (defaultZoneOffset.isPresent()) {
                builder.withDefaultZoneOffset(defaultZoneOffset.get());
            }
        } else {
            if (defaultZoneId.isPresent()) {
                builder.withDefaultZoneId(defaultZoneId.get());
            } else if (defaultZoneOffset.isPresent()) {
                builder.withDefaultZoneOffset(defaultZoneOffset.get());
            }
            builder.withDefaultDate(defaultYear.orElse(1970), defaultMonthOfYear.orElse(1), defaultDayOfMonth.orElse(1));
        }
        this.formatter = builder.build();
    }

    public TimestampFormatterImpl(final String pattern, final String defaultZone, final String defaultDate) {
        final org.embulk.util.timestamp.Builder builder = org.embulk.util.timestamp.TimestampFormatter.builder(pattern, true);
        builder.setDefaultZoneFromString(defaultZone);
        builder.setDefaultDateFromString(defaultDate);
        this.formatter = builder.build();
    }

    @Override
    public Instant parse(final String text) {
        return this.formatter.parse(text);
    }

    private final TimestampFormatter formatter;
}
