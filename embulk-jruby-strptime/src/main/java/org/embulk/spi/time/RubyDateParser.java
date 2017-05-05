package org.embulk.spi.time;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.jruby.RubyString;
import org.embulk.spi.time.lexer.StrptimeLexer;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;
import org.jruby.util.RubyTimeOutputFormatter;

import static org.jruby.RubyRational.newRationalCanonicalize;

/**
 * This is Java implementation of ext/date/date_strptime.c in Ruby v2.3.x.
 */
public class RubyDateParser
{
    // day_names
    private static final String[] DAY_NAMES = new String[] {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    // month_names
    private static final String[] MONTH_NAMES = new String[] {
            "January", "February", "March", "April", "May", "June", "July", "August", "September",
            "October", "November", "December", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    // merid_names
    private static final String[] MERID_NAMES = new String[] {
            "am", "pm", "a.m.", "p.m."
    };

    // Ported Date::Format::Bag from lib/ruby/stdlib/date/format.rb in JRuby
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

        public HashMap<String, Object> toMap(ThreadContext context)
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
                int offset = RubyDateParse.dateZoneToDiff(zone);
                if (offset != Integer.MIN_VALUE) {
                    map.put("offset", offset);
                }
            }
            if (has(sec_fraction)) {
                map.put("sec_fraction", newRationalCanonicalize(context, sec_fraction, (long)Math.pow(10, sec_fraction_size)));
            }
            if (hasSeconds()) {
                if (has(seconds_size)) {
                    map.put("seconds", newRationalCanonicalize(context, seconds, (long) Math.pow(10, seconds_size)));
                }
                else {
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

        public boolean setYearIfNotSet(int v)
        {
            if (has(year)) {
                return false;
            }
            else {
                year = v;
                return true;
            }
        }

        public boolean setMonthIfNotSet(int v)
        {
            if (has(mon)) {
                return false;
            }
            else {
                mon = v;
                return true;
            }
        }

        public boolean setMdayIfNotSet(int v)
        {
            if (has(mday)) {
                return false;
            }
            else {
                mday = v;
                return true;
            }
        }

        public boolean hasSeconds()
        {
            return seconds != Long.MIN_VALUE;
        }

        public static boolean has(int v)
        {
            return v != Integer.MIN_VALUE;
        }
    }

    private final ThreadContext context;
    private final StrptimeLexer lexer;

    public RubyDateParser(ThreadContext context)
    {
        this.context = context;
        this.lexer = new StrptimeLexer((Reader) null);
    }

    // Ported from org.jruby.util.RubyDateFormatter#addToPattern
    private void addToPattern(final List<StrptimeToken> compiledPattern, final String str)
    {
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
                compiledPattern.add(StrptimeToken.format(c));
            }
            else {
                compiledPattern.add(StrptimeToken.str(Character.toString(c)));
            }
        }
    }

    // Ported from org.jruby.util.RubyDateFormatter#compilePattern
    public List<StrptimeToken> compilePattern(final RubyString format, final boolean dateLibrary)
    {
        return compilePattern(format.getByteList(), dateLibrary);
    }

    // Ported from org.jruby.util.RubyDateFormatter#compilePattern
    public List<StrptimeToken> compilePattern(final ByteList pattern, final boolean dateLibrary)
    {
        final List<StrptimeToken> compiledPattern = new LinkedList<>();

        final Encoding enc = pattern.getEncoding();
        if (!enc.isAsciiCompatible()) {
            throw context.runtime.newArgumentError("format should have ASCII compatible encoding");
        }
        if (enc != ASCIIEncoding.INSTANCE) { // default for ByteList
            compiledPattern.add(new StrptimeToken(StrptimeFormat.FORMAT_ENCODING, enc));
        }

        ByteArrayInputStream in = new ByteArrayInputStream(pattern.getUnsafeBytes(), pattern.getBegin(), pattern.getRealSize());
        Reader reader = new InputStreamReader(in, context.runtime.getEncodingService().charsetForEncoding(pattern.getEncoding()));
        lexer.yyreset(reader);

        StrptimeToken token;
        try {
            while ((token = lexer.yylex()) != null) {
                if (token.getFormat() != StrptimeFormat.FORMAT_SPECIAL) {
                    compiledPattern.add(token);
                }
                else {
                    char c = (Character) token.getData();
                    switch (c) {
                        case 'c':
                            addToPattern(compiledPattern, "a b e H:M:S Y");
                            break;
                        case 'D':
                        case 'x':
                            addToPattern(compiledPattern, "m/d/y");
                            break;
                        case 'F':
                            addToPattern(compiledPattern, "Y-m-d");
                            break;
                        case 'n':
                            compiledPattern.add(StrptimeToken.str("\n"));
                            break;
                        case 'Q':
                            if (dateLibrary) {
                                compiledPattern.add(new StrptimeToken(StrptimeFormat.FORMAT_MICROSEC_EPOCH));
                            }
                            else {
                                compiledPattern.add(StrptimeToken.str("%Q"));
                            }
                            break;
                        case 'R':
                            addToPattern(compiledPattern, "H:M");
                            break;
                        case 'r':
                            addToPattern(compiledPattern, "I:M:S p");
                            break;
                        case 'T':
                        case 'X':
                            addToPattern(compiledPattern, "H:M:S");
                            break;
                        case 't':
                            compiledPattern.add(StrptimeToken.str("\t"));
                            break;
                        case 'v':
                            addToPattern(compiledPattern, "e-");
                            if (!dateLibrary)
                                compiledPattern.add(
                                    StrptimeToken.formatter(new RubyTimeOutputFormatter("^", 0)));
                            addToPattern(compiledPattern, "b-Y");
                            break;
                        case 'Z':
                            if (dateLibrary) {
                                // +HH:MM in 'date', never zone name
                                compiledPattern.add(StrptimeToken.zoneOffsetColons(1));
                            }
                            else {
                                compiledPattern.add(new StrptimeToken(StrptimeFormat.FORMAT_ZONE_ID));
                            }
                            break;
                        case '+':
                            if (!dateLibrary) {
                                compiledPattern.add(StrptimeToken.str("%+"));
                                break;
                            }
                            addToPattern(compiledPattern, "a b e H:M:S ");
                            // %Z: +HH:MM in 'date', never zone name
                            compiledPattern.add(StrptimeToken.zoneOffsetColons(1));
                            addToPattern(compiledPattern, " Y");
                            break;
                        default:
                            throw new Error("Unknown special char: " + c);
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return compiledPattern;
    }

    // Ported Date._strptime_i method in lib/ruby/stdlib/date/format.rb in JRuby
    public FormatBag parse(final List<StrptimeToken> compiledPattern, final String text)
    {
        final FormatBag bag = new StringParser(text).parse(compiledPattern);
        if (bag == null) {
            return null;
        }

        if (FormatBag.has(bag._cent)) {
            if (FormatBag.has(bag.cwyear)) {
                bag.cwyear += bag._cent * 100;
            }
            if (FormatBag.has(bag.year)) {
                bag.year += bag._cent * 100;
            }

            // delete bag._cent
            bag._cent = Integer.MIN_VALUE;
        }

        if (FormatBag.has(bag._merid)) {
            if (FormatBag.has(bag.hour)) {
                bag.hour %= 12;
                bag.hour += bag._merid;
            }

            // delete bag._merid
            bag._merid = Integer.MIN_VALUE;
        }

        return bag;
    }

    // Ported from Date._strptime method in lib/ruby/stdlib/date/format.rb in JRuby
    // This is Java implementation of date__strptime method in ext/date/date_strptime.c in Ruby
    public HashMap<String, Object> parse(final String format, final String text)
    {
        final List<StrptimeToken> compiledPattern = compilePattern(context.runtime.newString(format), true);
        final FormatBag bag = parse(compiledPattern, text);
        if (bag != null) {
            return bag.toMap(context);
        }
        else {
            return null;
        }
    }

    private static class StringParser
    {
        private static final Pattern ZONE_PARSE_REGEX = Pattern.compile("\\A(" +
                        "(?:gmt|utc?)?[-+]\\d+(?:[,.:]\\d+(?::\\d+)?)?" +
                        "|(?-i:[[\\p{Alpha}].\\s]+)(?:standard|daylight)\\s+time\\b" +
                        "|(?-i:[[\\p{Alpha}]]+)(?:\\s+dst)?\\b" +
                        ")", Pattern.CASE_INSENSITIVE);

        private final String text;
        private final FormatBag bag;

        private int pos;
        private boolean fail;

        private StringParser(String text)
        {
            this.text = text;
            this.bag = new FormatBag();

            this.pos = 0;
            this.fail = false;
        }

        private FormatBag parse(final List<StrptimeToken> compiledPattern)
        {
            for (int tokenIndex = 0; tokenIndex < compiledPattern.size(); tokenIndex++) {
                final StrptimeToken token = compiledPattern.get(tokenIndex);

                switch (token.getFormat()) {
                    case FORMAT_ENCODING: {
                        continue; // skip
                    }
                    case FORMAT_OUTPUT: {
                        continue; // skip
                    }
                    case FORMAT_STRING: {
                        final String str = token.getData().toString();
                        for (int i = 0; i < str.length(); i++) {
                            final char c = str.charAt(i);
                            if (isSpace(c)) {
                                while (!isEndOfText(text, pos) && isSpace(text.charAt(pos))) {
                                    pos++;
                                }
                            }
                            else {
                                if (isEndOfText(text, pos) || c != text.charAt(pos)) {
                                    fail = true;
                                }
                                pos++;
                            }
                        }
                        break;
                    }
                    case FORMAT_WEEK_LONG: // %A - The full weekday name (``Sunday'')
                    case FORMAT_WEEK_SHORT: { // %a - The abbreviated name (``Sun'')
                        final int dayIndex = findIndexInPatterns(DAY_NAMES);
                        if (dayIndex >= 0) {
                            bag.wday = dayIndex % 7;
                            pos += DAY_NAMES[dayIndex].length();
                        }
                        else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_MONTH_LONG: // %B - The full month name (``January'')
                    case FORMAT_MONTH_SHORT: { // %b, %h - The abbreviated month name (``Jan'')
                        final int monIndex = findIndexInPatterns(MONTH_NAMES);
                        if (monIndex >= 0) {
                            bag.mon = monIndex % 12 + 1;
                            pos += MONTH_NAMES[monIndex].length();
                        }
                        else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_CENTURY: { // %C - year / 100 (round down.  20 in 2009)
                        final long cent;
                        if (isNumberPattern(compiledPattern, tokenIndex)) {
                            cent = readDigits(2);
                        }
                        else {
                            cent = readDigitsMax();
                        }
                        bag._cent = (int)cent;
                        break;
                    }
                    case FORMAT_DAY: // %d, %Od - Day of the month, zero-padded (01..31)
                    case FORMAT_DAY_S: { // %e, %Oe - Day of the month, blank-padded ( 1..31)
                        final long day;
                        if (isBlank(text, pos)) {
                            pos += 1; // blank
                            day = readDigits(1);
                        }
                        else {
                            day = readDigits(2);
                        }

                        if (!validRange(day, 1, 31)) {
                            fail = true;
                        }
                        bag.mday = (int)day;
                        break;
                    }
                    case FORMAT_WEEKYEAR: { // %G - The week-based year
                        final long year;
                        if (isNumberPattern(compiledPattern, tokenIndex)) {
                            year = readDigits(4);
                        }
                        else {
                            year = readDigitsMax();
                        }
                        bag.cwyear = (int)year;
                        break;
                    }
                    case FORMAT_WEEKYEAR_SHORT: { // %g - The last 2 digits of the week-based year (00..99)
                        final long v = readDigits(2);
                        if (!validRange(v, 0, 99)) {
                            fail = true;
                        }
                        bag.cwyear = (int)v;
                        if (!bag.has(bag._cent)) {
                            bag._cent = v >= 69 ? 19 : 20;
                        }
                        break;
                    }
                    case FORMAT_HOUR: // %H, %OH - Hour of the day, 24-hour clock, zero-padded (00..23)
                    case FORMAT_HOUR_BLANK: { // %k - Hour of the day, 24-hour clock, blank-padded ( 0..23)
                        final long hour;
                        if (isBlank(text, pos)) {
                            pos += 1; // blank
                            hour = readDigits(1);
                        }
                        else {
                            hour = readDigits(2);
                        }

                        if (!validRange(hour, 0, 24)) {
                            fail = true;
                        }
                        bag.hour = (int)hour;
                        break;
                    }
                    case FORMAT_HOUR_M: // %I, %OI - Hour of the day, 12-hour clock, zero-padded (01..12)
                    case FORMAT_HOUR_S: { // %l - Hour of the day, 12-hour clock, blank-padded ( 1..12)
                        final long hour;
                        if (isBlank(text, pos)) {
                            pos += 1; // blank
                            hour = readDigits(1);
                        }
                        else {
                            hour = readDigits(2);
                        }

                        if (!validRange(hour, 1, 12)) {
                            fail = true;
                        }
                        bag.hour = (int)hour;
                        break;
                    }
                    case FORMAT_DAY_YEAR: { // %j - Day of the year (001..366)
                        final long day = readDigits(3);
                        if (!validRange(day, 1, 365)) {
                            fail = true;
                        }
                        bag.yday = (int)day;
                        break;
                    }
                    case FORMAT_MILLISEC: // %L - Millisecond of the second (000..999)
                    case FORMAT_NANOSEC: { // %N - Fractional seconds digits, default is 9 digits (nanosecond)
                        boolean negative = false;
                        if (isSign(text, pos)) {
                            negative = text.charAt(pos) == '-';
                            pos++;
                        }

                        final long v;
                        final int initPos = pos;
                        if (isNumberPattern(compiledPattern, tokenIndex)) {
                            if (token.getFormat() == StrptimeFormat.FORMAT_MILLISEC) {
                                v = readDigits(3);
                            }
                            else {
                                v = readDigits(9);
                            }
                        }
                        else {
                            v = readDigitsMax();
                        }

                        bag.sec_fraction = (int)(!negative ? v : -v);
                        bag.sec_fraction_size = pos - initPos;
                        break;
                    }
                    case FORMAT_MINUTES: { // %M, %OM - Minute of the hour (00..59)
                        final long min = readDigits(2);
                        if (!validRange(min, 0, 59)) {
                            fail = true;
                        }
                        bag.min = (int)min;
                        break;
                    }
                    case FORMAT_MONTH: { // %m, %Om - Month of the year, zero-padded (01..12)
                        final long mon = readDigits(2);
                        if (!validRange(mon, 1, 12)) {
                            fail = true;
                        }
                        bag.mon = (int)mon;
                        break;
                    }
                    case FORMAT_MERIDIAN: // %P - Meridian indicator, lowercase (``am'' or ``pm'')
                    case FORMAT_MERIDIAN_LOWER_CASE: { // %p - Meridian indicator, uppercase (``AM'' or ``PM'')
                        final int meridIndex = findIndexInPatterns(MERID_NAMES);
                        if (meridIndex >= 0) {
                            bag._merid = meridIndex % 2 == 0 ? 0 : 12;
                            pos += MERID_NAMES[meridIndex].length();
                        }
                        else {
                            fail = true;
                        }
                        break;
                    }
                    case FORMAT_MICROSEC_EPOCH: { // %Q - Number of microseconds since 1970-01-01 00:00:00 UTC.
                        boolean negative = false;
                        if (isMinus(text, pos)) {
                            negative = true;
                            pos++;
                        }

                        final long sec = readDigitsMax();
                        bag.seconds = !negative ? sec : -sec;
                        bag.seconds_size = 3;
                        break;
                    }
                    case FORMAT_SECONDS: { // %S - Second of the minute (00..59)
                        final long sec = readDigits(2);
                        if (!validRange(sec, 0, 60)) {
                            fail = true;
                        }
                        bag.sec = (int)sec;
                        break;
                    }
                    case FORMAT_EPOCH: { // %s - Number of seconds since 1970-01-01 00:00:00 UTC.
                        boolean negative = false;
                        if (isMinus(text, pos)) {
                            negative = true;
                            pos++;
                        }

                        final long sec = readDigitsMax();
                        bag.seconds = (int)(!negative ? sec : -sec);
                        break;
                    }
                    case FORMAT_WEEK_YEAR_S: // %U, %OU - Week number of the year.  The week starts with Sunday.  (00..53)
                    case FORMAT_WEEK_YEAR_M: { // %W, %OW - Week number of the year.  The week starts with Monday.  (00..53)
                        final long week = readDigits(2);
                        if (!validRange(week, 0, 53)) {
                            fail = true;
                        }

                        if (token.getFormat() == StrptimeFormat.FORMAT_WEEK_YEAR_S) {
                            bag.wnum0 = (int)week;
                        } else {
                            bag.wnum1 = (int)week;
                        }
                        break;
                    }
                    case FORMAT_DAY_WEEK2: { // %u, %Ou - Day of the week (Monday is 1, 1..7)
                        final long day = readDigits(1);
                        if (!validRange(day, 1, 7)) {
                            fail = true;
                        }
                        bag.cwday = (int)day;
                        break;
                    }
                    case FORMAT_WEEK_WEEKYEAR: { // %V, %OV - Week number of the week-based year (01..53)
                        final long week = readDigits(2);
                        if (!validRange(week, 1, 53)) {
                            fail = true;
                        }
                        bag.cweek = (int)week;
                        break;
                    }
                    case FORMAT_DAY_WEEK: { // %w - Day of the week (Sunday is 0, 0..6)
                        final long day = readDigits(1);
                        if (!validRange(day, 0, 6)) {
                            fail = true;
                        }
                        bag.wday = (int)day;
                        break;
                    }
                    case FORMAT_YEAR_LONG: {
                        // %Y, %EY - Year with century (can be negative, 4 digits at least)
                        //           -0001, 0000, 1995, 2009, 14292, etc.
                        boolean negative = false;
                        if (isSign(text, pos)) {
                            negative = text.charAt(pos) == '-';
                            pos++;
                        }

                        final long year;
                        if (isNumberPattern(compiledPattern, tokenIndex)) {
                            year = readDigits(4);
                        } else {
                            year = readDigitsMax();
                        }

                        bag.year = (int)(!negative ? year : -year);
                        break;
                    }
                    case FORMAT_YEAR_SHORT: { // %y, %Ey, %Oy - year % 100 (00..99)
                        final long y = readDigits(2);
                        if (!validRange(y, 0, 99)) {
                            fail = true;
                        }
                        bag.year = (int)y;
                        if (!bag.has(bag._cent)) {
                            bag._cent = y >= 69 ? 19 : 20;
                        }
                        break;
                    }
                    case FORMAT_ZONE_ID: // %Z - Time zone abbreviation name
                    case FORMAT_COLON_ZONE_OFF: {
                        // %z - Time zone as hour and minute offset from UTC (e.g. +0900)
                        //      %:z - hour and minute offset from UTC with a colon (e.g. +09:00)
                        //      %::z - hour, minute and second offset from UTC (e.g. +09:00:00)
                        //      %:::z - hour, minute and second offset from UTC
                        //          (e.g. +09, +09:30, +09:30:30)
                        if (isEndOfText(text, pos)) {
                            fail = true;
                            break;
                        }

                        final Matcher m = ZONE_PARSE_REGEX.matcher(text.substring(pos));
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

            return bag;
        }

        // Ported read_digits from ext/date/date_strptime.c
        private long readDigits(final int len)
        {
            char c;
            long v = 0;
            final int initPos = pos;

            for (int i = 0; i < len; i++) {
                if (isEndOfText(text, pos)) {
                    break;
                }

                c = text.charAt(pos);
                if (!isDigit(c)) {
                    break;
                }
                else {
                    v = v * 10 + toInt(c);
                }
                pos += 1;
            }

            if (pos == initPos) {
                fail = true;
            }

            return v;
        }

        // Ported READ_DIGITS_MAX from ext/date/date_strptime.c
        private long readDigitsMax()
        {
            return readDigits(Integer.MAX_VALUE);
        }

        /**
         * Returns -1 if text doesn't match with patterns.
         */
        private int findIndexInPatterns(final String[] patterns)
        {
            if (isEndOfText(text, pos)) {
                return -1;
            }

            for (int i = 0; i < patterns.length; i++) {
                final String pattern = patterns[i];
                final int len = pattern.length();
                if (!isEndOfText(text, pos + len - 1)
                        && pattern.equalsIgnoreCase(text.substring(pos, pos + len))) { // strncasecmp
                    return i;
                }
            }

            return -1; // text doesn't match at any patterns.
        }

        // Ported num_pattern_p from ext/date/date_strptime.c
        private static boolean isNumberPattern(final List<StrptimeToken> compiledPattern, final int i)
        {
            if (compiledPattern.size() <= i + 1) {
                return false;
            }
            else {
                final StrptimeToken nextToken = compiledPattern.get(i + 1);
                final StrptimeFormat f = nextToken.getFormat();
                if (f == StrptimeFormat.FORMAT_STRING && isDigit(((String) nextToken.getData()).charAt(0))) {
                    return true;
                }
                else if (NUMBER_PATTERNS.contains(f)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        // CDdeFGgHIjkLlMmNQRrSsTUuVvWwXxYy
        private static final EnumSet<StrptimeFormat> NUMBER_PATTERNS =
                EnumSet.copyOf(Arrays.asList(
                        StrptimeFormat.FORMAT_CENTURY, // 'C'
                        // D
                        StrptimeFormat.FORMAT_DAY, // 'd'
                        StrptimeFormat.FORMAT_DAY_S, // 'e'
                        // F
                        StrptimeFormat.FORMAT_WEEKYEAR, // 'G'
                        StrptimeFormat.FORMAT_WEEKYEAR_SHORT, // 'g'
                        StrptimeFormat.FORMAT_HOUR, // 'H'
                        StrptimeFormat.FORMAT_HOUR_M, // 'I'
                        StrptimeFormat.FORMAT_DAY_YEAR, // 'j'
                        StrptimeFormat.FORMAT_HOUR_BLANK, // 'k'
                        StrptimeFormat.FORMAT_MILLISEC, // 'L'
                        StrptimeFormat.FORMAT_HOUR_S, // 'l'
                        StrptimeFormat.FORMAT_MINUTES, // 'M'
                        StrptimeFormat.FORMAT_MONTH, // 'm'
                        StrptimeFormat.FORMAT_NANOSEC, // 'N'
                        // Q, R, r
                        StrptimeFormat.FORMAT_SECONDS, // 'S'
                        StrptimeFormat.FORMAT_EPOCH, // 's'
                        // T
                        StrptimeFormat.FORMAT_WEEK_YEAR_S, // 'U'
                        StrptimeFormat.FORMAT_DAY_WEEK2, // 'u'
                        StrptimeFormat.FORMAT_WEEK_WEEKYEAR, // 'V'
                        // v
                        StrptimeFormat.FORMAT_WEEK_YEAR_M, // 'W'
                        StrptimeFormat.FORMAT_DAY_WEEK, // 'w'
                        // X, x
                        StrptimeFormat.FORMAT_YEAR_LONG, // 'Y'
                        StrptimeFormat.FORMAT_YEAR_SHORT // 'y'
                ));

        // Ported valid_range_p from ext/date/date_strptime.c
        private static boolean validRange(long v, int lower, int upper)
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

        private static boolean isDigit(char c)
        {
            return '0' <= c && c <= '9';
        }

        private static boolean isEndOfText(String text, int pos)
        {
            return pos >= text.length();
        }

        private static boolean isSign(String text, int pos)
        {
            return !isEndOfText(text, pos) && (text.charAt(pos) == '+' || text.charAt(pos) == '-');
        }

        private static boolean isMinus(String text, int pos)
        {
            return !isEndOfText(text, pos) && text.charAt(pos) == '-';
        }

        private static boolean isBlank(String text, int pos)
        {
            return !isEndOfText(text, pos) && text.charAt(pos) == ' ';
        }

        private static int toInt(char c)
        {
            return c - '0';
        }
    }
}
