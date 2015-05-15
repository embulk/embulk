package org.jruby.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class Temporal // TODO better naming
{
    // @see https://github.com/jruby/jruby/blob/master/core/src/main/java/org/jruby/RubyTime.java#L1366
    public static Temporal newTemporal(ParsedValues values)
    {
        long sec = 0;
        int nsec = 0;

        if (values.seconds != Integer.MIN_VALUE) {
            int u = 0;
            if (values.sec_fraction != Integer.MIN_VALUE) {
                if (values.sec_fraction_rational == 1000) {
                    u = values.sec_fraction * 1000;
                } else { // 1000_000_000
                    u = values.sec_fraction;
                }
            }

            int seconds;
            if (values.seconds_rational != Integer.MIN_VALUE) { // Rational
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
            if (values.mon != Integer.MIN_VALUE) {
                dt.plusMonths(values.mon - 1);
            }
            if (values.mday != Integer.MIN_VALUE) {
                dt.plusDays(values.mday - 1);
            }
            if (values.hour != Integer.MIN_VALUE) {
                dt.plusHours(values.hour);
            }
            if (values.min != Integer.MIN_VALUE) {
                dt.plusMinutes(values.min);
            }
            if (values.sec != Integer.MIN_VALUE) {
                dt.plusMinutes(values.sec);
            }

            if (values.sec_fraction != Integer.MIN_VALUE) {
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
