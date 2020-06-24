package org.embulk.spi.time;

import java.time.Instant;

public class Timestamp implements Comparable<Timestamp> {
    private Timestamp(final Instant instant) {
        this.instant = instant;
    }

    public static Timestamp ofString(final String string) {
        return new Timestamp(Instants.parseInstant(string));
    }

    public static Timestamp ofInstant(final Instant instant) {
        return new Timestamp(instant);
    }

    public static Timestamp ofEpochSecond(final long epochSecond) {
        return new Timestamp(Instant.ofEpochSecond(epochSecond));
    }

    public static Timestamp ofEpochSecond(final long epochSecond, final long nanoAdjustment) {
        return new Timestamp(Instant.ofEpochSecond(epochSecond, nanoAdjustment));
    }

    public static Timestamp ofEpochMilli(final long epochMilli) {
        return new Timestamp(Instant.ofEpochMilli(epochMilli));
    }

    public Instant getInstant() {
        return this.instant;
    }

    public long getEpochSecond() {
        return this.instant.getEpochSecond();
    }

    public int getNano() {
        return this.instant.getNano();
    }

    public long toEpochMilli() {
        return this.instant.toEpochMilli();
    }

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

    @Override
    public int hashCode() {
        return this.instant.hashCode() ^ 0x55555555;
    }

    @Override
    public int compareTo(final Timestamp other) {
        return this.instant.compareTo(other.instant);
    }

    @Override
    public String toString() {
        return Instants.toString(this.instant);
    }

    private final Instant instant;
}
