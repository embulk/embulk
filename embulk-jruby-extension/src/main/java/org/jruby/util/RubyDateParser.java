package org.jruby.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.Ruby;
import org.jruby.RubyString;
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

    private int matchAtPatterns(String text, int pos, String[] patterns)
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

    private static boolean isValidRange(long v, int lower, int upper)
    {
        return lower <= v && v <= upper;
    }

    private static boolean isSpace(char c)
    {
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

    private static Set<Format> numPatterns; // CDdeFGgHIjkLlMmNQRrSsTUuVvWwXxYy
    static {
        numPatterns = new HashSet<>();
        numPatterns.add(Format.FORMAT_CENTURY); // 'C'
        // D
        numPatterns.add(Format.FORMAT_DAY); // 'd'
        numPatterns.add(Format.FORMAT_DAY_S); // 'e'
        // F
        numPatterns.add(Format.FORMAT_WEEKYEAR); // 'G'
        numPatterns.add(Format.FORMAT_WEEKYEAR_SHORT); // 'g'
        numPatterns.add(Format.FORMAT_HOUR); // 'H'
        numPatterns.add(Format.FORMAT_HOUR_M); // 'I'
        numPatterns.add(Format.FORMAT_DAY_YEAR); // 'j'
        numPatterns.add(Format.FORMAT_HOUR_BLANK); // 'k'
        numPatterns.add(Format.FORMAT_MILLISEC); // 'L'
        numPatterns.add(Format.FORMAT_HOUR_S); // 'l'
        numPatterns.add(Format.FORMAT_MINUTES); // 'M'
        numPatterns.add(Format.FORMAT_MONTH); // 'm'
        numPatterns.add(Format.FORMAT_NANOSEC); // 'N'
        // Q, R, r
        numPatterns.add(Format.FORMAT_SECONDS); // 'S'
        numPatterns.add(Format.FORMAT_EPOCH); // 's'
        // T
        numPatterns.add(Format.FORMAT_WEEK_YEAR_S); // 'U'
        numPatterns.add(Format.FORMAT_DAY_WEEK2); // 'u'
        numPatterns.add(Format.FORMAT_WEEK_WEEKYEAR); // 'V'
        // v
        numPatterns.add(Format.FORMAT_WEEK_YEAR_M); // 'W'
        numPatterns.add(Format.FORMAT_DAY_WEEK); // 'w'
        // X, x
        numPatterns.add(Format.FORMAT_YEAR_LONG); // 'Y'
        numPatterns.add(Format.FORMAT_YEAR_SHORT); // 'y'
    }

    private static boolean matchAtNumPatterns(Token token) // NUM_PATTERN_P
    {
        if (token == null) {
            return false;
        }

        // TODO
        Format f = token.getFormat();
        if (f == Format.FORMAT_STRING && isDigit(((String)token.getData()).charAt(0))) {
            return true;

        } else if (numPatterns.contains(f)) {
            return true;

        }
        return false;
    }

    private static RuntimeException newInvalidDataException() // Token token, int v
    {
        return new RuntimeException("Invalid Data"); // TODO InvalidDataException
    }

    private final ThreadContext context;
    // Use RubyDateFormatter temporarily because it has useful lexer, token and format types
    private final RubyDateFormatter dateFormat;

    private List<Token> compiledPattern;
    private int pos;
    private String text;

    public RubyDateParser()
    {
        this(Ruby.newInstance().getCurrentContext());
    }

    public RubyDateParser(ThreadContext context)
    {
        this.context = context;
        this.dateFormat = new RubyDateFormatter(context);
    }

    public List<Token> compilePattern(String format)
    {
        return compilePattern(context.runtime.newString(format), true);
    }

    public List<Token> compilePattern(RubyString format, boolean dateLibrary)
    {
        return dateFormat.compilePattern(format, dateLibrary);
    }

    private static Token getToken(List<Token> compiledPattern, int index)
    {
        return compiledPattern.get(index);
    }

    private static Token nextToken(List<Token> compiledPattern, int index)
    {
        if (compiledPattern.size() <= index + 1) {
            return null;
        } else {
            return compiledPattern.get(index + 1);
        }
    }

    // TODO RubyTime parse(RubyString format, RubyString text);
    // TODO RubyTime parse(List<Token> compiledPattern, RubyString text);

    public Temporal date_strptime(List<Token> compiledPattern, String text)
    {
        ParsedValues values = date_strptime_internal(compiledPattern, text);
        return Temporal.newTemporal(values);
    }

    ParsedValues date_strptime_internal(String format, String text)
    {
        List<Token> compiledPattern = compilePattern(context.runtime.newString(format), true);
        return date_strptime_internal(compiledPattern, text);
    }

    ParsedValues date_strptime_internal(List<Token> compiledPattern, String text)
    {
        pos = 0;
        this.text = text;

        ParsedValues values = new ParsedValues();

        for (int i = 0; i < compiledPattern.size(); i++) {
            Token token = getToken(compiledPattern, i);

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
                            values.fail();
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
                    int dayIndex = matchAtPatterns(text, pos, dayNames);
                    if (dayIndex >= 0) {
                        values.wday = dayIndex % 7;
                        pos += dayNames[dayIndex].length();
                    } else {
                        values.fail();
                    }
                    break;
                }
                case FORMAT_MONTH_LONG: // %B - The full month name (``January'')
                case FORMAT_MONTH_SHORT: // %b, %h - The abbreviated month name (``Jan'')
                {
                    int monIndex = matchAtPatterns(text, pos, monNames);
                    if (monIndex >= 0) {
                        values.mon = monIndex % 12 + 1;
                        pos += monNames[monIndex].length();
                    } else {
                        values.fail();
                    }
                    break;
                }
                case FORMAT_CENTURY: // %C - year / 100 (round down.  20 in 2009)
                {
                    long c;
                    if (matchAtNumPatterns(nextToken(compiledPattern, i))) {
                        c = readDigits(2);
                    } else {
                        c = readDigitsMax();
                    }
                    values._cent = (int)c;
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

                    if (!isValidRange(d, 1, 31)) {
                        values.fail();
                    }
                    values.mday = (int)d;
                    break;
                }
                case FORMAT_WEEKYEAR: // %G - The week-based year
                {
                    long y;
                    if (matchAtNumPatterns(nextToken(compiledPattern, i))) {
                        y = readDigits(4);
                    } else {
                        y = readDigitsMax();
                    }
                    values.cwyear = (int)y;
                    break;
                }
                case FORMAT_WEEKYEAR_SHORT: // %g - The last 2 digits of the week-based year (00..99)
                {
                    long v = readDigits(2);
                    if (!isValidRange(v, 0, 99)) {
                        values.fail();
                    }
                    values.cwyear = (int)v;
                    if (!values.has(values._cent)) {
                        values._cent = v >= 69 ? 19 : 20;
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

                    if (!isValidRange(h, 0, 24)) {
                        values.fail();
                    }
                    values.hour = (int)h;
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

                    if (!isValidRange(h, 1, 12)) {
                        values.fail();
                    }
                    values.hour = (int)h;
                    break;
                }
                case FORMAT_DAY_YEAR: // %j - Day of the year (001..366)
                {
                    long d = readDigits(3);
                    if (!isValidRange(d, 1, 365)) {
                        values.fail();
                    }
                    values.yday = (int)d;
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
                    if (matchAtNumPatterns(nextToken(compiledPattern, i))) {
                        if (token.getFormat() == Format.FORMAT_MILLISEC) {
                            v = readDigits(3);
                        } else {
                            v = readDigits(9);
                        }
                    } else {
                        v = readDigitsMax();
                    }

                    values.sec_fraction = (int)(!negative ? v : -v);
                    values.sec_fraction_size = pos - init_pos;
                    break;
                }
                case FORMAT_MINUTES: // %M, %OM - Minute of the hour (00..59)
                {
                    long min = readDigits(2);
                    if (!isValidRange(min, 0, 59)) {
                        values.fail();
                    }
                    values.min = (int)min;
                    break;
                }
                case FORMAT_MONTH: // %m, %Om - Month of the year, zero-padded (01..12)
                {
                    long mon = readDigits(2);
                    if (!isValidRange(mon, 1, 12)) {
                        values.fail();
                    }
                    values.mon = (int)mon;
                    break;
                }
                case FORMAT_MERIDIAN: // %P - Meridian indicator, lowercase (``am'' or ``pm'')
                case FORMAT_MERIDIAN_LOWER_CASE: // %p - Meridian indicator, uppercase (``AM'' or ``PM'')
                {
                    int meridIndex = matchAtPatterns(text, pos, meridNames);
                    if (meridIndex >= 0) {
                        values._merid = meridIndex % 2 == 0 ? 0 : 12;
                        pos += meridNames[meridIndex].length();
                    } else {
                        values.fail();
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
                    values.seconds = !negative ? sec : -sec;
                    values.seconds_size = 3;
                    break;
                }
                case FORMAT_SECONDS: // %S - Second of the minute (00..59)
                {
                    long sec = readDigits(2);
                    if (!isValidRange(sec, 0, 60)) {
                        values.fail();
                    }
                    values.sec = (int)sec;
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
                    values.seconds = (int)(!negative ? sec : -sec);
                    break;
                }
                case FORMAT_WEEK_YEAR_S: // %U, %OU - Week number of the year.  The week starts with Sunday.  (00..53)
                case FORMAT_WEEK_YEAR_M: // %W, %OW - Week number of the year.  The week starts with Monday.  (00..53)
                {
                    long w = readDigits(2);
                    if (!isValidRange(w, 0, 53)) {
                        values.fail();
                    }

                    if (token.getFormat() == Format.FORMAT_WEEK_YEAR_S) {
                        values.wnum0 = (int)w;
                    } else {
                        values.wnum1 = (int)w;
                    }
                    break;
                }
                case FORMAT_DAY_WEEK2: // %u, %Ou - Day of the week (Monday is 1, 1..7)
                {
                    long d = readDigits(1);
                    if (!isValidRange(d, 1, 7)) {
                        values.fail();
                    }
                    values.cwday = (int)d;
                    break;
                }
                case FORMAT_WEEK_WEEKYEAR: // %V, %OV - Week number of the week-based year (01..53)
                {
                    long w = readDigits(2);
                    if (!isValidRange(w, 1, 53)) {
                        values.fail();
                    }
                    values.cweek = (int)w;
                    break;
                }
                case FORMAT_DAY_WEEK: // %w - Day of the week (Sunday is 0, 0..6)
                {
                    long d = readDigits(1);
                    if (!isValidRange(d, 0, 6)) {
                        values.fail();
                    }
                    values.wday = (int)d;
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
                    if (matchAtNumPatterns(nextToken(compiledPattern, i))) {
                        y = readDigits(4);
                    } else {
                        y = readDigitsMax();
                    }

                    values.year = (int)(!negative ? y : -y);
                    break;
                }
                case FORMAT_YEAR_SHORT: // %y, %Ey, %Oy - year % 100 (00..99)
                {
                    long y = readDigits(2);
                    if (!isValidRange(y, 0, 99)) {
                        values.fail();
                    }
                    values.year = (int)y;
                    if (!values.has(values._cent)) {
                        values._cent = y >= 69 ? 19 : 20;
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
                        values.zone = zone;
                        pos += zone.length();

                        // TODO not calcurate offset here
                        //// offset
                        //hash.put("offset", ParsedValues.toDiff(zone));
                    } else {
                        values.fail();
                    }
                    break;
                }
                case FORMAT_SPECIAL:
                {
                    throw new Error("FORMAT_SPECIAL is a special token only for the lexer.");
                }
            }
        }

        if (values.fail) {
            return null;
        }

        if (text.length() > pos) {
            values.leftover = text.substring(pos, text.length());
        }

        if (values.has(values._cent)) {
            if (values.has(values.cwyear)) {
                values.cwyear += values._cent * 100;
            }
            if (values.has(values.year)) {
                values.year += values._cent * 100;
            }

            // delete values._cent
            values._cent = Integer.MIN_VALUE;
        }

        if (values.has(values._merid)) {
            if (values.has(values.hour)) {
                values.hour %= 12;
                values.hour += values._merid;
            }

            // delete values._merid
            values._merid = Integer.MIN_VALUE;
        }

        return values;
    }

    private long readDigits(int len)
    {
        long v = 0;
        try {
            for (int i = 0; i < len; i++) {
                char c = text.charAt(pos); // IndexOutOfBounds
                if (!isDigit(c)) {
                    if (i > 0) {
                        break;
                    } else {
                        throw newInvalidDataException();
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
}