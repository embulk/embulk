package org.jruby.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class Temporal // TODO better naming
{
    public static Temporal newTemporal(ParsedValues values)
    {
        // TODO
        // use DateTime to convert TimeHash-like-internal-class to Temporal.
        // See https://github.com/jruby/jruby/blob/master/core/src/main/java/org/jruby/RubyTime.java#L1366

        long sec = 0;
        int nsec = 0;

        if (values.seconds >= 0) {
            int u = 0;
            if (values.sec_fraction >= 0) {
                if (values.sec_fraction_rational == 1000) {
                    u = values.sec_fraction * 1000;
                } else { // 1000_000_000
                    u = values.sec_fraction;
                }
            }

            int seconds;
            if (values.seconds_rational >= 0) { // Rational
                seconds = values.seconds * 1000000;
            } else { // 1000 // int
                seconds = values.seconds * 1000;
            }

            sec = (seconds + u) / 1000000;
            nsec = u * 1000;
        } else {
            DateTimeZone dtz = DateTimeZone.UTC;

            int year = 1970; // 1970, 1, 1, 0, 0, 0, 0
            if (values.year >= 0) {
                year = values.year;
            }

            // set up with min values and then add to allow rolling over
            DateTime dt = new DateTime(year, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
            if (values.mon >= 0) {
                dt.plusMonths(values.mon - 1);
            }
            if (values.mday >= 0) {
                dt.plusDays(values.mday - 1);
            }
            if (values.hour >= 0) {
                dt.plusHours(values.hour);
            }
            if (values.min >= 0) {
                dt.plusMinutes(values.min);
            }
            if (values.sec >= 0) {
                dt.plusMinutes(values.sec);
            }

            if (values.sec_fraction >= 0) {
                int u;
                if (values.sec_fraction_rational == 1000) {
                    u = values.sec_fraction * 1000;
                } else { // 1000_000_000
                    u = values.sec_fraction;
                }
                dt.plusMillis(u * 1000 % 1000);
                nsec = u * 1000000000 % 1000000;
            }

            dt = dt.withZoneRetainFields(dtz);
            sec = dt.getMillis() / 1000;
        }

        return new Temporal(sec, nsec, values.zone);
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
