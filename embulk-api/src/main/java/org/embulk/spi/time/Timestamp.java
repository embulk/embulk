/*
 * Copyright 2014 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.spi.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an instance of Embulk's Timestamp type.
 *
 * <p>Embulk needed this till v0.8 because v0.8 expected Java 7, which does not have {@link java.time.Instant}.
 * Embulk v0.9 requires Java 8, and it expects {@link java.time.Instant}. {@link org.embulk.spi.time.Timestamp}
 * remains here for compatibility.
 *
 * @deprecated Use {@link java.time.Instant} instead as much as possible.
 *
 * @since 0.4.0
 */
@Deprecated
public class Timestamp implements Comparable<Timestamp> {
    private Timestamp(final Instant instant) {
        this.instant = instant;
    }

    /**
     * @since 0.10.6
     */
    public static Timestamp ofString(final String string) {
        return new Timestamp(parseInstant(string));
    }

    /**
     * @since 0.9.0
     */
    public static Timestamp ofInstant(final Instant instant) {
        return new Timestamp(instant);
    }

    /**
     * @since 0.4.0
     */
    public static Timestamp ofEpochSecond(final long epochSecond) {
        return new Timestamp(Instant.ofEpochSecond(epochSecond));
    }

    /**
     * @since 0.4.0
     */
    public static Timestamp ofEpochSecond(final long epochSecond, final long nanoAdjustment) {
        return new Timestamp(Instant.ofEpochSecond(epochSecond, nanoAdjustment));
    }

    /**
     * @since 0.4.0
     */
    public static Timestamp ofEpochMilli(final long epochMilli) {
        return new Timestamp(Instant.ofEpochMilli(epochMilli));
    }

    /**
     * @since 0.9.0
     */
    public Instant getInstant() {
        return this.instant;
    }

    /**
     * @since 0.4.0
     */
    public long getEpochSecond() {
        return this.instant.getEpochSecond();
    }

    /**
     * @since 0.4.0
     */
    public int getNano() {
        return this.instant.getNano();
    }

    /**
     * @since 0.4.0
     */
    public long toEpochMilli() {
        return this.instant.toEpochMilli();
    }

    /**
     * @since 0.4.0
     */
    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof Timestamp)) {
            return false;
        }
        final Timestamp other = (Timestamp) otherObject;
        return this.instant.equals(other.instant);
    }

    /**
     * @since 0.4.0
     */
    @Override
    public int hashCode() {
        return this.instant.hashCode() ^ 0x55555555;
    }

    /**
     * @since 0.4.0
     */
    @Override
    public int compareTo(final Timestamp other) {
        return this.instant.compareTo(other.instant);
    }

    /**
     * @since 0.4.0
     */
    @Override
    public String toString() {
        return toString(this.instant);
    }

    private static String toString(final Instant instant) {
        // This is the same implementation with org.embulk.spi.time.Instants#toString(Instant),
        // but this org.embulk.spi.time.Timestamp has to have its own implementation because
        //
        // 1) org.embulk.spi.time.Timestamp is moved to embulk-api, which does not depend on others.
        // 2) org.embulk.spi.time.Timestamp is deprecated, and to be removed in the (remote) future.
        //    So, embulk-core should not call Timestamp#toString.
        //
        // The duplication would be resolved when we can finally remove org.embulk.spi.time.Timestamp.

        if (instant == null) {
            throw new NullPointerException("Timestamp owns a null Instant.");
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

    private static Instant parseInstant(final String string) {
        // This is the same implementation with org.embulk.spi.time.Instants#parseInstant(String),
        // but this org.embulk.spi.time.Timestamp has to have its own implementation because
        //
        // 1) org.embulk.spi.time.Timestamp is moved to embulk-api, which does not depend on others.
        // 2) org.embulk.spi.time.Timestamp is deprecated, and to be removed in the (remote) future.
        //    So, embulk-core should not call Timestamp#ofString.
        //
        // The duplication would be resolved when we can finally remove org.embulk.spi.time.Timestamp.

        if (string == null) {
            throw new NullPointerException("Timestamp#ofString receives only a non-null String.");
        }

        final Matcher matcher = INSTANT_PATTERN.matcher(string);
        if (!matcher.matches()) {
            throw new NumberFormatException("Timestamp#ofString received an invalid format: '" + string + "'");
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

    private final Instant instant;
}
