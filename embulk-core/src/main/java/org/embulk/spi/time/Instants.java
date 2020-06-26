package org.embulk.spi.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for {@code java.time.Instant}.
 *
 * <p>Plugins should not use those methods directly.
 */
public final class Instants {
    private Instants() {
        // Pass-through.
    }

    public static String toString(final Instant instant) {
        if (instant == null) {
            throw new NullPointerException("Instants.toString receives only a non-null Instant.");
        }

        final int nano = instant.getNano();
        if (nano == 0) {
            return INSTANT_FORMATTER_SECONDS.format(instant) + " UTC";
        } else if (nano % 1000000 == 0) {
            return INSTANT_FORMATTER_MILLISECONDS.format(instant) + " UTC";
        } else {
            final StringBuilder builder = new StringBuilder();
            INSTANT_FORMATTER_SECONDS.formatTo(instant, builder);
            builder.append(".");

            final String digits;
            final int zeroDigits;
            if (nano % 1000 == 0) {
                digits = Integer.toString(nano / 1000);
                zeroDigits = 6 - digits.length();
            } else {
                digits = Integer.toString(nano);
                zeroDigits = 9 - digits.length();
            }
            builder.append(digits);
            for (int i = 0; i < zeroDigits; i++) {
                builder.append('0');
            }

            builder.append(" UTC");
            return builder.toString();
        }
    }

    public static Instant parseInstant(final String string) {  // from TimestampSerDe
        if (string == null) {
            throw new NullPointerException("Instants.parseInstant receives only a non-null String.");
        }

        final Matcher matcher = INSTANT_PATTERN.matcher(string);
        if (!matcher.matches()) {
            throw new NumberFormatException("Instants.parseInstant received an invalid format: '" + string + "'");
        }

        // If the string matches INSTANT_PATTERN, all parsing below should be successful.
        // Any Exception would be considered as an unexpected error.

        final String integral;
        try {
            integral = matcher.group(1);
        } catch (final Exception ex) {
            throw new IllegalStateException("Unexpected error in retrieving the integral part of: '" + string + "'", ex);
        }

        final long epochSecond;
        try {
            epochSecond = LocalDateTime.parse(integral, INSTANT_FORMATTER_SECONDS).toEpochSecond(ZoneOffset.UTC);
        } catch (final Exception ex) {
            throw new IllegalStateException("Unexpected error in parsing: '" + integral + "'", ex);
        }

        final String fractional;
        try {
            fractional = matcher.group(2);
        } catch (final Exception ex) {
            throw new IllegalStateException("Unexpected error in retrieving the fractional part of: '" + string + "'", ex);
        }

        final int nanoAdjustment;
        if (fractional == null) {
            nanoAdjustment = 0;
        } else {
            try {
                nanoAdjustment = Integer.parseInt(fractional) * (int) Math.pow(10, 9 - fractional.length());
            } catch (final Exception ex) {
                throw new IllegalStateException("Unexpected error in parsing: '" + fractional + "'", ex);
            }
        }

        return Instant.ofEpochSecond(epochSecond, nanoAdjustment);
    }

    private static final Pattern INSTANT_PATTERN =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(?:\\.(\\d{1,9}))? (?:UTC|\\+?00\\:?00)");

    private static final DateTimeFormatter INSTANT_FORMATTER_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter INSTANT_FORMATTER_MILLISECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);
}
