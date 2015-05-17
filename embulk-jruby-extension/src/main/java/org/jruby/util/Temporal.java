package org.jruby.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class Temporal // TODO better naming
{
    // @see https://github.com/jruby/jruby/blob/master/core/src/main/java/org/jruby/RubyTime.java#L1366
    public static Temporal newTemporal(ParsedValues values)
    {
        long sec;
        long nsec = 0;

        if (values.hasSeconds()) {
            if (values.has(values.sec_fraction)) {
                nsec = values.sec_fraction * (int)Math.pow(10, 9 - values.sec_fraction_size);
            }

            if (values.has(values.seconds_size)) { // Rational
                sec = values.seconds / (int)Math.pow(10, values.seconds_size);
            } else { // int
                sec = values.seconds;
            }

        } else {
            DateTimeZone dtz = DateTimeZone.UTC;

            int year;
            if (values.has(values.year)) {
                year = values.year;
            } else {
                year = 1970;
            }

            // set up with min values and then add to allow rolling over
            DateTime dt = new DateTime(year, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
            if (values.has(values.mon)) {
                dt = dt.plusMonths(values.mon - 1);
            }
            if (values.has(values.mday)) {
                dt = dt.plusDays(values.mday - 1);
            }
            if (values.has(values.hour)) {
                dt = dt.plusHours(values.hour);
            }
            if (values.has(values.min)) {
                dt = dt.plusMinutes(values.min);
            }
            if (values.has(values.sec)) {
                dt = dt.plusSeconds(values.sec);
            }

            if (values.has(values.sec_fraction)) {
                nsec = values.sec_fraction * (int)Math.pow(10, 9 - values.sec_fraction_size);
                dt = dt.plusMillis((int)nsec / 1000000);
            }

            dt = dt.withZoneRetainFields(dtz);
            sec = dt.getMillis() / 1000;
        }

        return new Temporal(sec, (int)nsec, values.zone);
    }

    private final long sec;
    private final int nsec;
    private final String zone;  // +0900, JST, UTC

    public Temporal(long sec, int nsec, String zone)
    {
        this.sec = sec;
        this.nsec = nsec;
        this.zone = zone;
    }

    public long getSec()
    {
        return sec;
    }

    public int getNsec()
    {
        return nsec;
    }

    public String getZone()
    {
        return zone;
    }
}
