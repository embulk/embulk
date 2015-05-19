package org.jruby.util;

import java.util.List;
import java.util.EnumSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.runtime.ThreadContext;

import static org.jruby.util.RubyDateFormatter.Format;
import static org.jruby.util.RubyDateFormatter.Token;

public class RubyDateParser
{
    private static String[] dayNames = new String[] {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    private static String[] monNames = new String[] {
            "January", "February", "March", "April", "May", "June", "July", "August", "September",
            "October", "November", "December", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private static String[] meridNames = new String[] {
            "am", "pm", "a.m.", "p.m."
    };

    private static final Pattern ZONE_PARSE_REGEX = Pattern.compile("\\A(" +
                    "(?:gmt|utc?)?[-+]\\d+(?:[,.:]\\d+(?::\\d+)?)?" +
                    "|(?-i:[[\\p{Alpha}].\\s]+)(?:standard|daylight)\\s+time\\b" +
                    "|(?-i:[[\\p{Alpha}]]+)(?:\\s+dst)?\\b" +
                    ")",
            Pattern.CASE_INSENSITIVE);

    // @see zones_source in date_parse.c
    // https://github.com/ruby/ruby/blob/trunk/ext/date/date_parse.c#L341
    private static final Map<String, Integer> ZONE_OFFSET_MAP;

    static {
        ZONE_OFFSET_MAP = new HashMap<>();
        ZONE_OFFSET_MAP.put("ut",   0*3600);
        ZONE_OFFSET_MAP.put("gmt",  0*3600);
        ZONE_OFFSET_MAP.put("est", -5*3600);
        ZONE_OFFSET_MAP.put("edt", -4*3600);
        ZONE_OFFSET_MAP.put("cst", -6*3600);
        ZONE_OFFSET_MAP.put("cdt", -5*3600);
        ZONE_OFFSET_MAP.put("mst", -7*3600);
        ZONE_OFFSET_MAP.put("mdt", -6*3600);
        ZONE_OFFSET_MAP.put("pst", -8*3600);
        ZONE_OFFSET_MAP.put("pdt", -7*3600);
        ZONE_OFFSET_MAP.put("a",    1*3600);
        ZONE_OFFSET_MAP.put("b",    2*3600);
        ZONE_OFFSET_MAP.put("c",    3*3600);
        ZONE_OFFSET_MAP.put("d",    4*3600);
        ZONE_OFFSET_MAP.put("e",    5*3600);
        ZONE_OFFSET_MAP.put("f",    6*3600);
        ZONE_OFFSET_MAP.put("g",    7*3600);
        ZONE_OFFSET_MAP.put("h",    8*3600);
        ZONE_OFFSET_MAP.put("i",    9*3600);
        ZONE_OFFSET_MAP.put("k",   10*3600);
        ZONE_OFFSET_MAP.put("l",   11*3600);
        ZONE_OFFSET_MAP.put("m",   12*3600);
        ZONE_OFFSET_MAP.put("n",   -1*3600);
        ZONE_OFFSET_MAP.put("o",   -2*3600);
        ZONE_OFFSET_MAP.put("p",   -3*3600);
        ZONE_OFFSET_MAP.put("q",   -4*3600);
        ZONE_OFFSET_MAP.put("r",   -5*3600);
        ZONE_OFFSET_MAP.put("s",   -6*3600);
        ZONE_OFFSET_MAP.put("t",   -7*3600);
        ZONE_OFFSET_MAP.put("u",   -8*3600);
        ZONE_OFFSET_MAP.put("v",   -9*3600);
        ZONE_OFFSET_MAP.put("w",  -10*3600);
        ZONE_OFFSET_MAP.put("x",  -11*3600);
        ZONE_OFFSET_MAP.put("y",  -12*3600);
        ZONE_OFFSET_MAP.put("z",    0*3600);
        ZONE_OFFSET_MAP.put("utc",  0*3600);
        ZONE_OFFSET_MAP.put("wet",  0*3600);
        ZONE_OFFSET_MAP.put("at",  -2*3600);
        ZONE_OFFSET_MAP.put("brst",-2*3600);
        ZONE_OFFSET_MAP.put("ndt", -(2*3600+1800));
        ZONE_OFFSET_MAP.put("art", -3*3600);
        ZONE_OFFSET_MAP.put("adt", -3*3600);
        ZONE_OFFSET_MAP.put("brt", -3*3600);
        ZONE_OFFSET_MAP.put("clst",-3*3600);
        ZONE_OFFSET_MAP.put("nst", -(3*3600+1800));
        ZONE_OFFSET_MAP.put("ast", -4*3600);
        ZONE_OFFSET_MAP.put("clt", -4*3600);
        ZONE_OFFSET_MAP.put("akdt",-8*3600);
        ZONE_OFFSET_MAP.put("ydt", -8*3600);
        ZONE_OFFSET_MAP.put("akst",-9*3600);
        ZONE_OFFSET_MAP.put("hadt",-9*3600);
        ZONE_OFFSET_MAP.put("hdt", -9*3600);
        ZONE_OFFSET_MAP.put("yst", -9*3600);
        ZONE_OFFSET_MAP.put("ahst",-10*3600);
        ZONE_OFFSET_MAP.put("cat",-10*3600);
        ZONE_OFFSET_MAP.put("hast",-10*3600);
        ZONE_OFFSET_MAP.put("hst",-10*3600);
        ZONE_OFFSET_MAP.put("nt",  -11*3600);
        ZONE_OFFSET_MAP.put("idlw",-12*3600);
        ZONE_OFFSET_MAP.put("bst",  1*3600);
        ZONE_OFFSET_MAP.put("cet",  1*3600);
        ZONE_OFFSET_MAP.put("fwt",  1*3600);
        ZONE_OFFSET_MAP.put("met",  1*3600);
        ZONE_OFFSET_MAP.put("mewt", 1*3600);
        ZONE_OFFSET_MAP.put("mez",  1*3600);
        ZONE_OFFSET_MAP.put("swt",  1*3600);
        ZONE_OFFSET_MAP.put("wat",  1*3600);
        ZONE_OFFSET_MAP.put("west", 1*3600);
        ZONE_OFFSET_MAP.put("cest", 2*3600);
        ZONE_OFFSET_MAP.put("eet",  2*3600);
        ZONE_OFFSET_MAP.put("fst",  2*3600);
        ZONE_OFFSET_MAP.put("mest", 2*3600);
        ZONE_OFFSET_MAP.put("mesz", 2*3600);
        ZONE_OFFSET_MAP.put("sast", 2*3600);
        ZONE_OFFSET_MAP.put("sst",  2*3600);
        ZONE_OFFSET_MAP.put("bt",   3*3600);
        ZONE_OFFSET_MAP.put("eat",  3*3600);
        ZONE_OFFSET_MAP.put("eest", 3*3600);
        ZONE_OFFSET_MAP.put("msk",  3*3600);
        ZONE_OFFSET_MAP.put("msd",  4*3600);
        ZONE_OFFSET_MAP.put("zp4",  4*3600);
        ZONE_OFFSET_MAP.put("zp5",  5*3600);
        ZONE_OFFSET_MAP.put("ist",  (5*3600+1800));
        ZONE_OFFSET_MAP.put("zp6",  6*3600);
        ZONE_OFFSET_MAP.put("wast", 7*3600);
        ZONE_OFFSET_MAP.put("cct",  8*3600);
        ZONE_OFFSET_MAP.put("sgt",  8*3600);
        ZONE_OFFSET_MAP.put("wadt", 8*3600);
        ZONE_OFFSET_MAP.put("jst",  9*3600);
        ZONE_OFFSET_MAP.put("kst",  9*3600);
        ZONE_OFFSET_MAP.put("east",10*3600);
        ZONE_OFFSET_MAP.put("gst", 10*3600);
        ZONE_OFFSET_MAP.put("eadt",11*3600);
        ZONE_OFFSET_MAP.put("idle",12*3600);
        ZONE_OFFSET_MAP.put("nzst",12*3600);
        ZONE_OFFSET_MAP.put("nzt", 12*3600);
        ZONE_OFFSET_MAP.put("nzdt",13*3600);
        ZONE_OFFSET_MAP.put("afghanistan",             16200);
        ZONE_OFFSET_MAP.put("alaskan",                -32400);
        ZONE_OFFSET_MAP.put("arab",                    10800);
        ZONE_OFFSET_MAP.put("arabian",                 14400);
        ZONE_OFFSET_MAP.put("arabic",                  10800);
        ZONE_OFFSET_MAP.put("atlantic",               -14400);
        ZONE_OFFSET_MAP.put("aus central",             34200);
        ZONE_OFFSET_MAP.put("aus eastern",             36000);
        ZONE_OFFSET_MAP.put("azores",                  -3600);
        ZONE_OFFSET_MAP.put("canada central",         -21600);
        ZONE_OFFSET_MAP.put("cape verde",              -3600);
        ZONE_OFFSET_MAP.put("caucasus",                14400);
        ZONE_OFFSET_MAP.put("cen. australia",          34200);
        ZONE_OFFSET_MAP.put("central america",        -21600);
        ZONE_OFFSET_MAP.put("central asia",            21600);
        ZONE_OFFSET_MAP.put("central europe",           3600);
        ZONE_OFFSET_MAP.put("central european",         3600);
        ZONE_OFFSET_MAP.put("central pacific",         39600);
        ZONE_OFFSET_MAP.put("central",                -21600);
        ZONE_OFFSET_MAP.put("china",                   28800);
        ZONE_OFFSET_MAP.put("dateline",               -43200);
        ZONE_OFFSET_MAP.put("e. africa",               10800);
        ZONE_OFFSET_MAP.put("e. australia",            36000);
        ZONE_OFFSET_MAP.put("e. europe",                7200);
        ZONE_OFFSET_MAP.put("e. south america",       -10800);
        ZONE_OFFSET_MAP.put("eastern",                -18000);
        ZONE_OFFSET_MAP.put("egypt",                    7200);
        ZONE_OFFSET_MAP.put("ekaterinburg",            18000);
        ZONE_OFFSET_MAP.put("fiji",                    43200);
        ZONE_OFFSET_MAP.put("fle",                      7200);
        ZONE_OFFSET_MAP.put("greenland",              -10800);
        ZONE_OFFSET_MAP.put("greenwich",                   0);
        ZONE_OFFSET_MAP.put("gtb",                      7200);
        ZONE_OFFSET_MAP.put("hawaiian",               -36000);
        ZONE_OFFSET_MAP.put("india",                   19800);
        ZONE_OFFSET_MAP.put("iran",                    12600);
        ZONE_OFFSET_MAP.put("jerusalem",                7200);
        ZONE_OFFSET_MAP.put("korea",                   32400);
        ZONE_OFFSET_MAP.put("mexico",                 -21600);
        ZONE_OFFSET_MAP.put("mid-atlantic",            -7200);
        ZONE_OFFSET_MAP.put("mountain",               -25200);
        ZONE_OFFSET_MAP.put("myanmar",                 23400);
        ZONE_OFFSET_MAP.put("n. central asia",         21600);
        ZONE_OFFSET_MAP.put("nepal",                   20700);
        ZONE_OFFSET_MAP.put("new zealand",             43200);
        ZONE_OFFSET_MAP.put("newfoundland",           -12600);
        ZONE_OFFSET_MAP.put("north asia east",         28800);
        ZONE_OFFSET_MAP.put("north asia",              25200);
        ZONE_OFFSET_MAP.put("pacific sa",             -14400);
        ZONE_OFFSET_MAP.put("pacific",                -28800);
        ZONE_OFFSET_MAP.put("romance",                  3600);
        ZONE_OFFSET_MAP.put("russian",                 10800);
        ZONE_OFFSET_MAP.put("sa eastern",             -10800);
        ZONE_OFFSET_MAP.put("sa pacific",             -18000);
        ZONE_OFFSET_MAP.put("sa western",             -14400);
        ZONE_OFFSET_MAP.put("samoa",                  -39600);
        ZONE_OFFSET_MAP.put("se asia",                 25200);
        ZONE_OFFSET_MAP.put("malay peninsula",         28800);
        ZONE_OFFSET_MAP.put("south africa",             7200);
        ZONE_OFFSET_MAP.put("sri lanka",               21600);
        ZONE_OFFSET_MAP.put("taipei",                  28800);
        ZONE_OFFSET_MAP.put("tasmania",                36000);
        ZONE_OFFSET_MAP.put("tokyo",                   32400);
        ZONE_OFFSET_MAP.put("tonga",                   46800);
        ZONE_OFFSET_MAP.put("us eastern",             -18000);
        ZONE_OFFSET_MAP.put("us mountain",            -25200);
        ZONE_OFFSET_MAP.put("vladivostok",             36000);
        ZONE_OFFSET_MAP.put("w. australia",            28800);
        ZONE_OFFSET_MAP.put("w. central africa",        3600);
        ZONE_OFFSET_MAP.put("w. europe",                3600);
        ZONE_OFFSET_MAP.put("west asia",               18000);
        ZONE_OFFSET_MAP.put("west pacific",            36000);
        ZONE_OFFSET_MAP.put("yakutsk",                 32400);
    }

    // CDdeFGgHIjkLlMmNQRrSsTUuVvWwXxYy
    private static EnumSet<Format> NUMBER_PATTERNS =
        EnumSet.copyOf(Arrays.asList(
                    Format.FORMAT_CENTURY, // 'C'
                    // D
                    Format.FORMAT_DAY, // 'd'
                    Format.FORMAT_DAY_S, // 'e'
                    // F
                    Format.FORMAT_WEEKYEAR, // 'G'
                    Format.FORMAT_WEEKYEAR_SHORT, // 'g'
                    Format.FORMAT_HOUR, // 'H'
                    Format.FORMAT_HOUR_M, // 'I'
                    Format.FORMAT_DAY_YEAR, // 'j'
                    Format.FORMAT_HOUR_BLANK, // 'k'
                    Format.FORMAT_MILLISEC, // 'L'
                    Format.FORMAT_HOUR_S, // 'l'
                    Format.FORMAT_MINUTES, // 'M'
                    Format.FORMAT_MONTH, // 'm'
                    Format.FORMAT_NANOSEC, // 'N'
                    // Q, R, r
                    Format.FORMAT_SECONDS, // 'S'
                    Format.FORMAT_EPOCH, // 's'
                    // T
                    Format.FORMAT_WEEK_YEAR_S, // 'U'
                    Format.FORMAT_DAY_WEEK2, // 'u'
                    Format.FORMAT_WEEK_WEEKYEAR, // 'V'
                    // v
                    Format.FORMAT_WEEK_YEAR_M, // 'W'
                    Format.FORMAT_DAY_WEEK, // 'w'
                    // X, x
                    Format.FORMAT_YEAR_LONG, // 'Y'
                    Format.FORMAT_YEAR_SHORT // 'y'
        ));

    public static class FormatBag
    {
        int mday = Integer.MIN_VALUE;
        int wday = Integer.MIN_VALUE;
        int cwday = Integer.MIN_VALUE;
        int yday = Integer.MIN_VALUE;
        int cweek = Integer.MIN_VALUE;
        int cwyear = Integer.MIN_VALUE;
        int min = Integer.MIN_VALUE;
        int mon = Integer.MIN_VALUE;
        int hour = Integer.MIN_VALUE;
        int year = Integer.MIN_VALUE;
        int sec = Integer.MIN_VALUE;
        int wnum0 = Integer.MIN_VALUE;
        int wnum1 = Integer.MIN_VALUE;

        String zone = null;

        int sec_fraction = Integer.MIN_VALUE; // Rational
        int sec_fraction_size = Integer.MIN_VALUE;

        long seconds = Long.MIN_VALUE; // long or Rational
        int seconds_size = Integer.MIN_VALUE;

        int _merid = Integer.MIN_VALUE;
        int _cent = Integer.MIN_VALUE;

        boolean fail = false;
        String leftover = null;

        void fail()
        {
            fail = true;
        }

        // @see https://github.com/jruby/jruby/blob/master/core/src/main/java/org/jruby/RubyTime.java#L1366
        public LocalTime makeLocalTime()
        {
            long sec_fraction_nsec = 0;
            if (has(this.sec_fraction)) {
                sec_fraction_nsec = this.sec_fraction * (int)Math.pow(10, 9 - this.sec_fraction_size);
            }

            long sec;
            if (hasSeconds()) {
                if (has(this.seconds_size)) { // Rational
                    sec = this.seconds / (int)Math.pow(10, this.seconds_size);
                } else { // int
                    sec = this.seconds;
                }

            } else {
                int year;
                if (has(this.year)) {
                    year = this.year;
                } else {
                    year = 1970;
                }

                // set up with min this and then add to allow rolling over
                DateTime dt = new DateTime(year, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
                if (has(this.mon)) {
                    dt = dt.plusMonths(this.mon - 1);
                }
                if (has(this.mday)) {
                    dt = dt.plusDays(this.mday - 1);
                }
                if (has(this.hour)) {
                    dt = dt.plusHours(this.hour);
                }
                if (has(this.min)) {
                    dt = dt.plusMinutes(this.min);
                }
                if (has(this.sec)) {
                    dt = dt.plusSeconds(this.sec);
                }
                sec = dt.getMillis() / 1000;
            }

            return new LocalTime(sec, sec_fraction_nsec, zone);
        }

        public HashMap<String, Object> toMap()
        {
            HashMap<String, Object> map = new HashMap<>();
            if (has(mday)) {
                map.put("mday", mday);
            }
            if (has(wday)) {
                map.put("wday", wday);
            }
            if (has(cwday)) {
                map.put("cwday", cwday);
            }
            if (has(yday)) {
                map.put("yday", yday);
            }
            if (has(cweek)) {
                map.put("cweek", cweek);
            }
            if (has(cwyear)) {
                map.put("cwyear", cwyear);
            }
            if (has(min)) {
                map.put("min", min);
            }
            if (has(mon)) {
                map.put("mon", mon);
            }
            if (has(hour)) {
                map.put("hour", hour);
            }
            if (has(year)) {
                map.put("year", year);
            }
            if (has(sec)) {
                map.put("sec", sec);
            }
            if (has(wnum0)) {
                map.put("wnum0", wnum0);
            }
            if (has(wnum1)) {
                map.put("wnum1", wnum1);
            }
            if (zone != null) {
                map.put("zone", zone);
                int offset = dateZoneToDiff(zone);
                if (offset != Integer.MIN_VALUE) {
                    map.put("offset", offset);
                }
            }
            if (has(sec_fraction)) {
                int sec_fraction_rational = (int)Math.pow(10, sec_fraction_size);
                map.put("sec_fraction", ((float) sec_fraction / sec_fraction_rational));
                // TODO return Rational
            }
            if (hasSeconds()) {
                if (has(seconds_size)) {
                    int seconds_rational = (int)Math.pow(10, seconds_size);
                    map.put("seconds", ((float) seconds / seconds_rational));
                } else {
                    map.put("seconds", seconds);
                }
            }
            if (has(_merid)) {
                map.put("_merid", _merid);
            }
            if (has(_cent)) {
                map.put("_cent", _cent);
            }
            if (leftover != null) {
                map.put("leftover", leftover);
            }

            return map;
        }

        private boolean hasSeconds()
        {
            return seconds != Long.MIN_VALUE;
        }

        private static boolean has(int v)
        {
            return v != Integer.MIN_VALUE;
        }
    }

    public static class LocalTime
    {
        private final long seconds;
        private final long nsecFraction;
        private final String zone;  // +0900, JST, UTC

        public LocalTime(long seconds, long nsecFraction, String zone)
        {
            this.seconds = seconds;
            this.nsecFraction = nsecFraction;
            this.zone = zone;
        }

        public long getSeconds()
        {
            return seconds;
        }

        public long getNsecFraction()
        {
            return nsecFraction;
        }

        public String getZone()
        {
            return zone;
        }
    }

    private final ThreadContext context;
    // Use RubyDateFormatter temporarily because it has useful lexer, token and format types
    private final RubyDateFormatter dateFormat;

    public RubyDateParser(ThreadContext context)
    {
        this.context = context;
        this.dateFormat = new RubyDateFormatter(context);
    }

    /** Convenience method when using no pattern caching */
    public RubyTime compileAndParse(RubyString format, boolean dateLibrary, RubyString text)
    {
        return parse(compilePattern(format, dateLibrary), text.decodeString());
    }

    public List<Token> compilePattern(RubyString format, boolean dateLibrary)
    {
        return dateFormat.compilePattern(format, dateLibrary);
    }

    public RubyTime parse(List<Token> compiledPattern, String text)
    {
        FormatBag bag = parseInternal(compiledPattern, text);
        LocalTime local = bag.makeLocalTime();
        long sec = local.getSeconds() + dateZoneToDiff(local.getZone());
        long msec = sec + local.getNsecFraction() / 1000000;
        int nsec = (int) (local.getNsecFraction() % 1000000);
        return RubyTime.newTime(context.runtime, new DateTime(msec, DateTimeZone.UTC), nsec);
    }

    public FormatBag parseInternal(String format, String text)
    {
        List<Token> compiledPattern = compilePattern(context.runtime.newString(format), true);
        return parseInternal(compiledPattern, text);
    }

    public FormatBag parseInternal(List<Token> compiledPattern, String text)
    {
        return Parser.parse(compiledPattern, text);
    }

    // @see date_zone_to_diff in date_parse.c
    // https://github.com/ruby/ruby/blob/trunk/ext/date/date_parse.c#L420
    public static int dateZoneToDiff(String zone)
    {
        String z = zone.toLowerCase();

        boolean dst;
        if (z.endsWith(" daylight time")) {
            z = z.substring(0, z.length() - " daylight time".length());
            dst = true;
        } else if (z.endsWith(" standard time")) {
            z = z.substring(0, z.length() - " standard time".length());
            dst = false;
        } else if (z.endsWith(" dst")) {
            z = z.substring(0, z.length() - " dst".length());
            dst = true;
        } else {
            dst = false;
        }

        if (ZONE_OFFSET_MAP.containsKey(z)) {
            int offset = ZONE_OFFSET_MAP.get(z);
            if (dst) {
                offset += 3600;
            }
            return offset;
        }

        if (z.startsWith("gmt") || z.startsWith("utc")) {
            z = z.substring(3, z.length()); // remove "gmt" or "utc"
        }

        boolean sign;
        if (z.charAt(0) == '+') {
            sign = true;
        } else if (z.charAt(0) == '-') {
            sign = false;
        } else {
            // if z doesn't start with "+" or "-", invalid
            return Integer.MIN_VALUE;
        }
        z = z.substring(1);

        int hour = 0, min = 0, sec = 0;
        if (z.contains(":")) {
            String[] splited = z.split(":");
            if (splited.length == 2) {
                hour = Integer.parseInt(splited[0]);
                min = Integer.parseInt(splited[1]);
            } else {
                hour = Integer.parseInt(splited[0]);
                min = Integer.parseInt(splited[1]);
                sec = Integer.parseInt(splited[2]);
            }

        } else if (z.contains(",") || z.contains(".")) {
            // TODO min = Rational(fr.to_i, 10**fr.size) * 60
            String[] splited = z.split("[\\.,]");
            hour = Integer.parseInt(splited[0]);
            min = (int)(Integer.parseInt(splited[1]) * 60 / Math.pow(10, splited[1].length()));

        } else {
            int len = z.length();
            if (len % 2 != 0) {
                if (len >= 1)
                    hour = Integer.parseInt(z.substring(0, 1));
                if (len >= 3)
                    min = Integer.parseInt(z.substring(1, 3));
                if (len >= 5)
                    sec = Integer.parseInt(z.substring(3, 5));

            } else {
                if (len >= 2)
                    hour = Integer.parseInt(z.substring(0, 2));
                if (len >= 4)
                    min = Integer.parseInt(z.substring(2, 4));
                if (len >= 6)
                    sec = Integer.parseInt(z.substring(4, 6));
            }
        }

        int offset = hour * 3600 + min * 60 + sec;
        return sign ? offset : -offset;
    }

    private static class Parser
    {
        public static FormatBag parse(List<Token> compiledPattern, String text)
        {
            return new Parser(text).parse(compiledPattern);
        }

        private final String text;
        private final FormatBag bag;
        private int pos;
        private boolean fail;

        private Parser(String text)
        {
            this.pos = 0;
            this.text = text;
            this.bag = new FormatBag();
            this.fail = false;
        }

        private FormatBag parse(List<Token> compiledPattern)
        {
            for (int i = 0; i < compiledPattern.size(); i++) {
                Token token = compiledPattern.get(i);

                switch (token.getFormat()) {
                    case FORMAT_ENCODING:
                        continue; // skip
                    case FORMAT_OUTPUT:
                        continue; // skip
                    case FORMAT_STRING:
                    {
                        String s = token.getData().toString();
                        for (int si = 0; si < s.length(); ) {
                            char sc = s.charAt(si);
                            if (isSpace(sc)) {
                                while (pos < text.length() && isSpace(text.charAt(pos))) {
                                    pos++;
                                }
                            } else if (pos >= text.length() || sc != text.charAt(pos)) {
                                fail = true;
                            } else {
                                pos++;
                            }
                            si++;
                        }
                        break;
                    }
                    case FORMAT_WEEK_LONG: // %A - The full weekday name (``Sunday'')
                    case FORMAT_WEEK_SHORT: // %a - The abbreviated name (``Sun'')
                    {
                        int dayIndex = matchPatternsAt(text, pos, dayNames);
                        if (dayIndex >= 0) {
                            bag.wday = dayIndex % 7;
                            pos += dayNames[dayIndex].length();
                        } else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_MONTH_LONG: // %B - The full month name (``January'')
                    case FORMAT_MONTH_SHORT: // %b, %h - The abbreviated month name (``Jan'')
                    {
                        int monIndex = matchPatternsAt(text, pos, monNames);
                        if (monIndex >= 0) {
                            bag.mon = monIndex % 12 + 1;
                            pos += monNames[monIndex].length();
                        } else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_CENTURY: // %C - year / 100 (round down.  20 in 2009)
                    {
                        long c;
                        if (isNextTokenNumberPattern(compiledPattern, i)) {
                            c = readDigits(2);
                        } else {
                            c = readDigitsMax();
                        }
                        bag._cent = (int)c;
                        break;
                    }
                    case FORMAT_DAY: // %d, %Od - Day of the month, zero-padded (01..31)
                    case FORMAT_DAY_S: // %e, %Oe - Day of the month, blank-padded ( 1..31)
                    {
                        long d;
                        if (text.charAt(pos) == ' ') { // brank or not
                            pos += 1; // brank
                            d = readDigits(1);
                        } else {
                            d = readDigits(2);
                        }

                        if (!isInRange(d, 1, 31)) {
                            fail = true;
                        }
                        bag.mday = (int)d;
                        break;
                    }
                    case FORMAT_WEEKYEAR: // %G - The week-based year
                    {
                        long y;
                        if (isNextTokenNumberPattern(compiledPattern, i)) {
                            y = readDigits(4);
                        } else {
                            y = readDigitsMax();
                        }
                        bag.cwyear = (int)y;
                        break;
                    }
                    case FORMAT_WEEKYEAR_SHORT: // %g - The last 2 digits of the week-based year (00..99)
                    {
                        long v = readDigits(2);
                        if (!isInRange(v, 0, 99)) {
                            fail = true;
                        }
                        bag.cwyear = (int)v;
                        if (!bag.has(bag._cent)) {
                            bag._cent = v >= 69 ? 19 : 20;
                        }
                        break;
                    }
                    case FORMAT_HOUR: // %H, %OH - Hour of the day, 24-hour clock, zero-padded (00..23)
                    case FORMAT_HOUR_BLANK: // %k - Hour of the day, 24-hour clock, blank-padded ( 0..23)
                    {
                        long h;
                        if (text.charAt(pos) == ' ') { // brank or not
                            pos += 1; // brank
                            h = readDigits(1);
                        } else {
                            h = readDigits(2);
                        }

                        if (!isInRange(h, 0, 24)) {
                            fail = true;
                        }
                        bag.hour = (int)h;
                        break;
                    }
                    case FORMAT_HOUR_M: // %I, %OI - Hour of the day, 12-hour clock, zero-padded (01..12)
                    case FORMAT_HOUR_S: // %l - Hour of the day, 12-hour clock, blank-padded ( 1..12)
                    {
                        long h;
                        if (text.charAt(pos) == ' ') { // brank or not
                            pos += 1; // brank
                            h = readDigits(1);
                        } else {
                            h = readDigits(2);
                        }

                        if (!isInRange(h, 1, 12)) {
                            fail = true;
                        }
                        bag.hour = (int)h;
                        break;
                    }
                    case FORMAT_DAY_YEAR: // %j - Day of the year (001..366)
                    {
                        long d = readDigits(3);
                        if (!isInRange(d, 1, 365)) {
                            fail = true;
                        }
                        bag.yday = (int)d;
                        break;
                    }
                    case FORMAT_MILLISEC: // %L - Millisecond of the second (000..999)
                    case FORMAT_NANOSEC: // %N - Fractional seconds digits, default is 9 digits (nanosecond)
                    {
                        long v;
                        boolean negative = false;

                        if (isSign(text.charAt(pos))) {
                            negative = text.charAt(pos) == '-';
                            pos++;
                        }

                        int init_pos = pos;
                        if (isNextTokenNumberPattern(compiledPattern, i)) {
                            if (token.getFormat() == Format.FORMAT_MILLISEC) {
                                v = readDigits(3);
                            } else {
                                v = readDigits(9);
                            }
                        } else {
                            v = readDigitsMax();
                        }

                        bag.sec_fraction = (int)(!negative ? v : -v);
                        bag.sec_fraction_size = pos - init_pos;
                        break;
                    }
                    case FORMAT_MINUTES: // %M, %OM - Minute of the hour (00..59)
                    {
                        long min = readDigits(2);
                        if (!isInRange(min, 0, 59)) {
                            fail = true;
                        }
                        bag.min = (int)min;
                        break;
                    }
                    case FORMAT_MONTH: // %m, %Om - Month of the year, zero-padded (01..12)
                    {
                        long mon = readDigits(2);
                        if (!isInRange(mon, 1, 12)) {
                            fail = true;
                        }
                        bag.mon = (int)mon;
                        break;
                    }
                    case FORMAT_MERIDIAN: // %P - Meridian indicator, lowercase (``am'' or ``pm'')
                    case FORMAT_MERIDIAN_LOWER_CASE: // %p - Meridian indicator, uppercase (``AM'' or ``PM'')
                    {
                        int meridIndex = matchPatternsAt(text, pos, meridNames);
                        if (meridIndex >= 0) {
                            bag._merid = meridIndex % 2 == 0 ? 0 : 12;
                            pos += meridNames[meridIndex].length();
                        } else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_MICROSEC_EPOCH: // %Q - Number of microseconds since 1970-01-01 00:00:00 UTC.
                    {
                        long sec;
                        boolean negative = false;

                        if (text.charAt(pos) == '-') {
                            negative = true;
                            pos++;
                        }

                        sec = readDigitsMax();
                        bag.seconds = !negative ? sec : -sec;
                        bag.seconds_size = 3;
                        break;
                    }
                    case FORMAT_SECONDS: // %S - Second of the minute (00..59)
                    {
                        long sec = readDigits(2);
                        if (!isInRange(sec, 0, 60)) {
                            fail = true;
                        }
                        bag.sec = (int)sec;
                        break;
                    }
                    case FORMAT_EPOCH: // %s - Number of seconds since 1970-01-01 00:00:00 UTC.
                    {
                        long sec;
                        boolean negative = false;

                        if (text.charAt(pos) == '-') {
                            negative = true;
                            pos++;
                        }
                        sec = readDigitsMax();
                        bag.seconds = (int)(!negative ? sec : -sec);
                        break;
                    }
                    case FORMAT_WEEK_YEAR_S: // %U, %OU - Week number of the year.  The week starts with Sunday.  (00..53)
                    case FORMAT_WEEK_YEAR_M: // %W, %OW - Week number of the year.  The week starts with Monday.  (00..53)
                    {
                        long w = readDigits(2);
                        if (!isInRange(w, 0, 53)) {
                            fail = true;
                        }

                        if (token.getFormat() == Format.FORMAT_WEEK_YEAR_S) {
                            bag.wnum0 = (int)w;
                        } else {
                            bag.wnum1 = (int)w;
                        }
                        break;
                    }
                    case FORMAT_DAY_WEEK2: // %u, %Ou - Day of the week (Monday is 1, 1..7)
                    {
                        long d = readDigits(1);
                        if (!isInRange(d, 1, 7)) {
                            fail = true;
                        }
                        bag.cwday = (int)d;
                        break;
                    }
                    case FORMAT_WEEK_WEEKYEAR: // %V, %OV - Week number of the week-based year (01..53)
                    {
                        long w = readDigits(2);
                        if (!isInRange(w, 1, 53)) {
                            fail = true;
                        }
                        bag.cweek = (int)w;
                        break;
                    }
                    case FORMAT_DAY_WEEK: // %w - Day of the week (Sunday is 0, 0..6)
                    {
                        long d = readDigits(1);
                        if (!isInRange(d, 0, 6)) {
                            fail = true;
                        }
                        bag.wday = (int)d;
                        break;
                    }
                    case FORMAT_YEAR_LONG:
                        // %Y, %EY - Year with century (can be negative, 4 digits at least)
                        //           -0001, 0000, 1995, 2009, 14292, etc.
                    {
                        boolean negative = false;

                        if (isSign(text.charAt(pos))) {
                            negative = text.charAt(pos) == '-';
                            pos++;
                        }

                        long y;
                        if (isNextTokenNumberPattern(compiledPattern, i)) {
                            y = readDigits(4);
                        } else {
                            y = readDigitsMax();
                        }

                        bag.year = (int)(!negative ? y : -y);
                        break;
                    }
                    case FORMAT_YEAR_SHORT: // %y, %Ey, %Oy - year % 100 (00..99)
                    {
                        long y = readDigits(2);
                        if (!isInRange(y, 0, 99)) {
                            fail = true;
                        }
                        bag.year = (int)y;
                        if (!bag.has(bag._cent)) {
                            bag._cent = y >= 69 ? 19 : 20;
                        }
                        break;
                    }
                    case FORMAT_ZONE_ID: // %Z - Time zone abbreviation name
                    case FORMAT_COLON_ZONE_OFF:
                        // %z - Time zone as hour and minute offset from UTC (e.g. +0900)
                        //      %:z - hour and minute offset from UTC with a colon (e.g. +09:00)
                        //      %::z - hour, minute and second offset from UTC (e.g. +09:00:00)
                        //      %:::z - hour, minute and second offset from UTC
                        //          (e.g. +09, +09:30, +09:30:30)
                    {
                        Matcher m = ZONE_PARSE_REGEX.matcher(text.substring(pos));
                        if (m.find()) {
                            // zone
                            String zone = text.substring(pos, pos + m.end());
                            bag.zone = zone;
                            pos += zone.length();
                        } else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_SPECIAL:
                    {
                        throw new Error("FORMAT_SPECIAL is a special token only for the lexer.");
                    }
                }
            }

            if (fail) {
                return null;
            }

            if (text.length() > pos) {
                bag.leftover = text.substring(pos, text.length());
            }

            if (bag.has(bag._cent)) {
                if (bag.has(bag.cwyear)) {
                    bag.cwyear += bag._cent * 100;
                }
                if (bag.has(bag.year)) {
                    bag.year += bag._cent * 100;
                }

                // delete bag._cent
                bag._cent = Integer.MIN_VALUE;
            }

            if (bag.has(bag._merid)) {
                if (bag.has(bag.hour)) {
                    bag.hour %= 12;
                    bag.hour += bag._merid;
                }

                // delete bag._merid
                bag._merid = Integer.MIN_VALUE;
            }

            return bag;
        }

        private long readDigits(int len)
        {
            long v = 0;
            int init_pos = pos;
            try {
                for (int i = 0; i < len; i++) {
                    char c = text.charAt(pos); // IndexOutOfBounds
                    if (!isDigit(c)) {
                        if (pos - init_pos != 0) {
                            break;
                        } else {
                            fail = true;
                        }
                    } else {
                        v = v * 10 + toInt(c);
                    }
                    pos += 1;
                }
            } catch (IndexOutOfBoundsException e) {
                // ignorable error
            }
            return v;
        }

        private long readDigitsMax()
        {
            return readDigits(Integer.MAX_VALUE);
        }

        private static int matchPatternsAt(String text, int pos, String[] patterns)
        {
            int patIndex = -1;
            for (int i = 0; i < patterns.length; i++) {
                String pattern = patterns[i];
                int len = pattern.length();
                try {
                    if (pattern.equalsIgnoreCase(text.substring(pos, pos + len))) { // IndexOutOfBounds
                        patIndex = i;
                        break;
                    }
                } catch (IndexOutOfBoundsException e) {
                    // ignorable error
                }
            }
            return patIndex;
        }

        private static boolean isNextTokenNumberPattern(List<Token> compiledPattern, int i)
        {
            if (compiledPattern.size() <= i + 1) {
                return false;
            } else {
                Token nextToken = compiledPattern.get(i + 1);

                Format f = nextToken.getFormat();
                if (f == Format.FORMAT_STRING && isDigit(((String)nextToken.getData()).charAt(0))) {
                    return true;
                } else if (NUMBER_PATTERNS.contains(f)) {
                    return true;
                }
                return false;
            }
        }

        private static boolean isInRange(long v, int lower, int upper)
        {
            return lower <= v && v <= upper;
        }

        private static boolean isSpace(char c)
        {
            // @see space characters are declared in date_strptime.c
            // https://github.com/ruby/ruby/blob/trunk/ext/date/date_strptime.c#L624
            return c == ' ' || c == '\t' || c == '\n' ||
                    c == '\u000b' || c == '\f' || c == '\r';
        }

        static boolean isDigit(char c)
        {
            return '0' <= c && c <= '9';
        }

        static boolean isSign(char c)
        {
            return c == '+' || c == '-';
        }

        static int toInt(char c)
        {
            return c - '0';
        }
    }
}
