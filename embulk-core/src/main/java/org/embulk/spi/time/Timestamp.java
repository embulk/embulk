package org.embulk.spi.time;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Timestamp
        implements Comparable<Timestamp>
{
    private Timestamp(final Instant instant)
    {
        this.instant = instant;
    }

    public static Timestamp ofInstant(final Instant instant)
    {
        return new Timestamp(instant);
    }

    public static Timestamp ofEpochSecond(final long epochSecond)
    {
        return new Timestamp(Instant.ofEpochSecond(epochSecond));
    }

    public static Timestamp ofEpochSecond(final long epochSecond, final long nanoAdjustment)
    {
        return new Timestamp(Instant.ofEpochSecond(epochSecond, nanoAdjustment));
    }

    public static Timestamp ofEpochMilli(final long epochMilli)
    {
        return new Timestamp(Instant.ofEpochMilli(epochMilli));
    }

    public Instant getInstant()
    {
        return this.instant;
    }

    public long getEpochSecond()
    {
        return this.instant.getEpochSecond();
    }

    public int getNano()
    {
        return this.instant.getNano();
    }

    public long toEpochMilli()
    {
        return this.instant.toEpochMilli();
    }

    @Override
    public boolean equals(final Object otherObject)
    {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof Timestamp)) {
            return false;
        }
        final Timestamp other = (Timestamp)otherObject;
        return this.instant.equals(other.instant);
    }

    @Override
    public int hashCode()
    {
        return this.instant.hashCode() ^ 0x55555555;
    }

    @Override
    public int compareTo(final Timestamp other)
    {
        return this.instant.compareTo(other.instant);
    }

    @Override
    public String toString()
    {
        final int nano = this.instant.getNano();
        if (nano == 0) {
            return FORMATTER_SECONDS.format(this.instant) + " UTC";
        } else if (nano % 1000000 == 0) {
            return FORMATTER_MILLISECONDS.format(this.instant) + " UTC";
        } else {
            final StringBuilder builder = new StringBuilder();
            FORMATTER_SECONDS.formatTo(this.instant, builder);
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

    private static final DateTimeFormatter FORMATTER_SECONDS =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FORMATTER_MILLISECONDS =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    private final Instant instant;
}
