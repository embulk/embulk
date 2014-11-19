package org.quickload.time;

public class Timestamp
{
    private final long seconds;
    private final int nano;

    private Timestamp(long seconds, int nano)
    {
        this.seconds = seconds;
        this.nano = nano;
    }

    public static Timestamp ofEpochSecond(long epochSecond)
    {
        return new Timestamp(epochSecond, 0);
    }

    public static Timestamp ofEpochSecond(long epochSecond, long nanoAdjustment)
    {
        return new Timestamp(epochSecond + nanoAdjustment / 1000000000, (int) (nanoAdjustment % 1000000000));
    }

    public static Timestamp ofEpochMilli(long epochMilli)
    {
        return new Timestamp(epochMilli / 1000, (int) (epochMilli % 1000 * 1000000));
    }

    public long getEpochSecond()
    {
        return seconds;
    }

    public int getNano()
    {
        return nano;
    }

    public long toEpochMilli()
    {
        return seconds * 1000 + nano / 1000000;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Timestamp)) {
            return false;
        }
        Timestamp o = (Timestamp) other;
        return this.seconds == o.seconds && this.nano == o.nano;
    }

    @Override
    public int hashCode()
    {
        int h = (int) (seconds ^ (seconds >>> 32));
        h += 17 * nano;
        return h;
    }
}
