package org.embulk.spi.time;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.DateTime;
import org.jruby.Ruby;
import org.jruby.RubyTime;

public class Timestamp
        implements Comparable<Timestamp>
{
    private final static DateTimeFormatter TO_STRING_FORMATTER_SECONDS = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss ").withZoneUTC();
    private final static DateTimeFormatter TO_STRING_FORMATTER_MILLIS = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS ").withZoneUTC();
    private final static DateTimeFormatter TO_STRING_FORMATTER_CUSTOM = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZoneUTC();
    private static final Pattern FROM_STRING_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})(?:\\.(\\d{1,9}))? (?:UTC|\\+?00\\:?00)");

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

    @Override
    public int compareTo(Timestamp t)
    {
        if (seconds < t.seconds) {
            return -1;
        } else if (seconds == t.seconds) {
            return nano == t.nano ? 0 : (nano < t.nano ? -1 : 1);
        } else {
            return 1;
        }
    }

    public RubyTime getRubyTime(Ruby runtime)
    {
        RubyTime time = new RubyTime(runtime, runtime.getClass("Time"), new DateTime(toEpochMilli())).gmtime();
        time.setNSec(nano % 1000000);
        return time;
    }

    public static Timestamp fromRubyTime(RubyTime time)
    {
        long msec = time.getDateTime().getMillis();
        long sec = msec / 1000;
        long nsec = time.getNSec() + (msec % 1000) * 1000000;
        return Timestamp.ofEpochSecond(sec, nsec);
    }

    @Override
    public String toString()
    {
        if (nano == 0) {
            return TO_STRING_FORMATTER_SECONDS.print(getEpochSecond() * 1000) + "UTC";

        } else if (nano % 1000000 == 0) {
            return TO_STRING_FORMATTER_MILLIS.print(toEpochMilli()) + "UTC";

        } else {
            StringBuffer sb = new StringBuffer();
            TO_STRING_FORMATTER_CUSTOM.printTo(sb, getEpochSecond() * 1000);
            sb.append(".");

            String digits;
            int zeroDigits;
            if (nano % 1000 == 0) {
                digits = Integer.toString(nano / 1000);
                zeroDigits = 6 - digits.length();
            } else {
                digits = Integer.toString(nano);
                zeroDigits = 9 - digits.length();
            }
            sb.append(digits);
            for (; zeroDigits > 0; zeroDigits--) {
                sb.append('0');
            }

            sb.append(" UTC");
            return sb.toString();
        }
    }

    static Timestamp fromString(String text)
    {
        // TODO exception handling
        Matcher m = FROM_STRING_PATTERN.matcher(text);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("Invalid timestamp format '%s'", text));
        }

        long seconds = TO_STRING_FORMATTER_CUSTOM.parseDateTime(m.group(1)).getMillis() / 1000;

        int nano;
        String frac = m.group(2);
        if (frac == null) {
            nano = 0;
        } else {
            nano = Integer.parseInt(frac) * (int) Math.pow(10, 9 - frac.length());
        }

        return new Timestamp(seconds, nano);
    }
}
