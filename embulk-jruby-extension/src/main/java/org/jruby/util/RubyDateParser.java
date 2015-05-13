package org.jruby.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern ZONE_PARSE_REGEX = Pattern.compile("\\A(" +
                    "(?:gmt|utc?)?[-+]\\d+(?:[,.:]\\d+(?::\\d+)?)?" +
                    "|(?-i:[[\\p{Alpha}].\\s]+)(?:standard|daylight)\\s+time\\b" +
                    "|(?-i:[[\\p{Alpha}]]+)(?:\\s+dst)?\\b" +
                    ")"
    );

    private static int toDiff(String zone)
    {
        String z = zone.toLowerCase();
        int offset = 0;

        boolean dst = false;
        if (z.endsWith(" daylight time")) {
            z = z.substring(0, z.length() - " daylight time".length());
            dst = true;

        } else if (z.endsWith(" standard time")) {
            z = z.substring(0, z.length() - " standard time".length());
            dst = false;

        } else if (z.endsWith(" dst")) {
            z = z.substring(0, z.length() - " dst".length());
            dst = true;
        }
        if (ZONE_OFFSET_MAP.containsKey(z)) {
            offset = ZONE_OFFSET_MAP.get(z);
            if (dst) {
                offset += 3600;
            }
            return offset;
        }

        if (z.startsWith("gmt") || z.startsWith("utc")) {
            z = z.substring(3, z.length()); // remove "gmt" or "utc"
        }

        if (!isSign(z.charAt(0))) {
            // if z doesn't have "+" or "-", invalid
            return 0;
        }

        boolean sign = false;
        sign = z.charAt(0) == '+';
        z = z.substring(1, z.length());

        int hour = 0, min = 0, sec = 0;
        if (z.contains(":")) {
            String[] splited = z.split(":");
            if (splited.length == 2) {
                hour = parseInt(splited[0]);
                min = parseInt(splited[1]);
            } else {
                hour = parseInt(splited[0]);
                min = parseInt(splited[1]);
                sec = parseInt(splited[2]);
            }
        } else if (z.contains(",") || z.contains(".")) {
            // TODO min = Rational(fr.to_i, 10**fr.size) * 60
            String[] splited = z.split("[\\.,]");
            hour = parseInt(splited[0]);
            min = (int)(parseInt(splited[1]) * 60 / Math.pow(10, splited[1].length()));

        } else {
            int len = z.length();
            if (len % 2 == 0) {
                if (len >= 1)
                    hour = parseInt(z.substring(0, 1));
                if (len >= 3)
                    min = parseInt(z.substring(1, 3));
                if (len >= 5)
                    min = parseInt(z.substring(3, 5));

            } else {
                if (len >= 2)
                    hour = parseInt(z.substring(0, 2));
                if (len >= 4)
                    min = parseInt(z.substring(2, 4));
                if (len >= 6)
                    min = parseInt(z.substring(4, 6));
            }
        }

        offset = hour * 3600 + min * 60 + sec;
        return sign ? offset : -offset;
    }

    private static int parseInt(String text)
    {
        int v = 0;
        try {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i); // IndexOutOfBounds
                if (!isDigit(c)) {
                    break;
                } else {
                    v += v * 10 + toInt(c);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // ignorable error
        }
        return v;
    }

    private static boolean validValueRange(int v, int lower, int upper)
    {
        return lower <= v && v <= upper;
    }

    private static boolean isDigit(char c)
    {
        return '0' <= c && c <= '9';
    }

    private static boolean isSign(char c)
    {
        return c == '+' || c == '-';
    }

    private static int toInt(char c)
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

    private List<Token> compiledPattern;

    private final ThreadContext context;
    // Use RubyDateFormatter temporarily because it has useful lexer, token and format types
    private final RubyDateFormatter dateFormat;

    private int pos;
    private String text;

    public RubyDateParser(ThreadContext context)
    {
        this.context = context;
        this.dateFormat = new RubyDateFormatter(context);
    }

    public List<Token> compilePattern(String format)
    {
        return compilePattern(context.runtime.newString(format), false);
    }

    public List<Token> compilePattern(RubyString format, boolean dateLibrary)
    {
        return dateFormat.compilePattern(format, dateLibrary);
    }

    // TODO RubyTime parse(RubyString format, RubyString text);
    // TODO RubyTime parse(List<Token> compiledPattern, RubyString text);

    public Temporal date_strptime(List<Token> compiledPattern, String text)
    {
        try {
            pos = 0;
            this.text = text;

            HashMap<String, Object> hash = date_strptime_internal(compiledPattern, text);
            if (text.length() > pos) {
                hash.put("leftover", text.substring(pos, text.length()));
            }

            return toTemporal(hash);
        } catch (RuntimeException e) {
            return null;
        }
    }

    Temporal toTemporal(HashMap<String, Object> hash)
    {
        // TODO
        // use DateTime to convert TimeHash-like-internal-class to Temporal.
        // See https://github.com/jruby/jruby/blob/master/core/src/main/java/org/jruby/RubyTime.java#L1366
        return null;
    }

    HashMap<String, Object> date_strptime_internal(List<Token> compiledPattern, String text)
    {
        HashMap<String, Object> hash = new HashMap<>();

        for (Token token : compiledPattern) {
            switch (token.getFormat()) {
                case FORMAT_ENCODING:
                    continue; // skip
                case FORMAT_OUTPUT:
                    continue; // skip
                case FORMAT_STRING:
                    pos += token.getData().toString().length();
                    break;
                case FORMAT_WEEK_LONG: // %A - The full weekday name (``Sunday'')
                case FORMAT_WEEK_SHORT: // %a - The abbreviated name (``Sun'')
                {
                    int dayIndex = matchAtPatterns(text, dayNames);
                    if (dayIndex >= 0) {
                        hash.put("wday", dayIndex % 7);
                        pos += dayNames[dayIndex].length();
                    } else {
                        throw newInvalidDataException();
                    }
                    break;
                }
                case FORMAT_MONTH_LONG: // %B - The full month name (``January'')
                case FORMAT_MONTH_SHORT: // %b, %h - The abbreviated month name (``Jan'')
                {
                    int monIndex = matchAtPatterns(text, monNames);
                    if (monIndex >= 0) {
                        hash.put("mon", monIndex % 12 + 1);
                        pos += monNames[monIndex].length();
                    } else {
                        throw newInvalidDataException();
                    }
                    break;
                }
                case FORMAT_CENTURY: // %C - year / 100 (round down.  20 in 2009)
                {
                    int c;
                    if (matchAtNumPatterns(token)) {
                        c = getDigits(2);
                    } else {
                        c = getDigits(Integer.MAX_VALUE);
                    }
                    hash.put("_cent", c);
                    break;
                }
                case FORMAT_DAY: // %d, %Od - Day of the month, zero-padded (01..31)
                case FORMAT_DAY_S: // %e, %Oe - Day of the month, blank-padded ( 1..31)
                {
                    int d;
                    if (text.charAt(pos) == ' ') { // brank or not
                        pos += 1; // brank
                        d = getDigits(1);
                    } else {
                        d = getDigits(2);
                    }

                    if (!validValueRange(d, 1, 31)) {
                        throw newInvalidDataException();
                    } else {
                        hash.put("mday", d);
                    }
                    break;
                }
                case FORMAT_WEEKYEAR: // %G - The week-based year
                {
                    int y;
                    if (matchAtNumPatterns(token)) {
                        y = getDigits(4);
                    } else {
                        y = getDigits(Integer.MAX_VALUE);
                    }
                    hash.put("cwyear", y);
                    break;
                }
                case FORMAT_WEEKYEAR_SHORT: // %g - The last 2 digits of the week-based year (00..99)
                {
                    int v = getDigits(2);
                    if (!validValueRange(v, 0, 99)) {
                        throw newInvalidDataException();
                    }
                    hash.put("cwyear", v);
                    if (hash.containsKey("_cent")) {
                        hash.put("_cent", v >= 69 ? 19 : 20);
                    }
                    break;
                }
                case FORMAT_HOUR: // %H, %OH - Hour of the day, 24-hour clock, zero-padded (00..23)
                case FORMAT_HOUR_BLANK: // %k - Hour of the day, 24-hour clock, blank-padded ( 0..23)
                {
                    int h;
                    if (text.charAt(pos) == ' ') { // brank or not
                        pos += 1; // brank
                        h = getDigits(1);
                    } else {
                        h = getDigits(2);
                    }

                    if (!validValueRange(h, 0, 23)) {
                        throw newInvalidDataException();
                    } else {
                        hash.put("hour", h);
                    }
                    break;
                }
                case FORMAT_HOUR_M: // %I, %OI - Hour of the day, 12-hour clock, zero-padded (01..12)
                case FORMAT_HOUR_S: // %l - Hour of the day, 12-hour clock, blank-padded ( 1..12)
                {
                    int h;
                    if (text.charAt(pos) == ' ') { // brank or not
                        pos += 1; // brank
                        h = getDigits(1);
                    } else {
                        h = getDigits(2);
                    }

                    if (!validValueRange(h, 1, 12)) {
                        throw newInvalidDataException();
                    } else {
                        hash.put("hour", h);
                    }
                    break;
                }
                case FORMAT_DAY_YEAR: // %j - Day of the year (001..366)
                {
                    int d = getDigits(3);
                    if (!validValueRange(d, 1, 366)) {
                        throw newInvalidDataException();
                    }
                    hash.put("yday", d);
                    break;
                }
                case FORMAT_MILLISEC: // %L - Millisecond of the second (000..999)
                case FORMAT_NANOSEC: // %N - Fractional seconds digits, default is 9 digits (nanosecond)
                {
                    int v;
                    boolean negative = false;
                    int num;

                    if (isSign(text.charAt(pos))) {
                        negative = text.charAt(pos) == '-';
                        pos++;
                    }

                    if (matchAtNumPatterns(token)) {
                        v = token.getFormat() == Format.FORMAT_MILLISEC ?
                                getDigits(3) : getDigits(9);
                    } else {
                        v = getDigits(Integer.MAX_VALUE);
                    }

                    if (!negative) {
                        v = -v;
                    }

                    /* TODO
                    set_hash("sec_fraction",
                             rb_rational_new2(n,
                                              f_expt(INT2FIX(10),
                                                     ULONG2NUM(si - osi))));
                     */

                    hash.put("_sec_fraction", !negative ? v : -v);
                    hash.put("_sec_fraction_rational", token.getFormat() == Format.FORMAT_MILLISEC ? 1000 : 1000000000);
                    break;
                }
                case FORMAT_MINUTES: // %M, %OM - Minute of the hour (00..59)
                {
                    int min = getDigits(2);
                    if (!validValueRange(min, 0, 59)) {
                        throw newInvalidDataException();
                    } else {
                        hash.put("min", min);
                    }
                    break;
                }
                case FORMAT_MONTH: // %m, %Om - Month of the year, zero-padded (01..12)
                {
                    int mon = getDigits(2);
                    if (!validValueRange(mon, 1, 12)) {
                        throw newInvalidDataException();
                    } else {
                        hash.put("mon", mon);
                    }
                    break;
                }
                case FORMAT_MERIDIAN: // %P - Meridian indicator, lowercase (``am'' or ``pm'')
                case FORMAT_MERIDIAN_LOWER_CASE: // %p - Meridian indicator, uppercase (``AM'' or ``PM'')
                {
                    int meridIndex = matchAtPatterns(text, meridNames);
                    if (meridIndex >= 0) {
                        hash.put("_merid", meridIndex % 2 == 0 ? 0 : 12);
                    } else {
                        throw newInvalidDataException();
                    }
                    break;
                }
                case FORMAT_MICROSEC_EPOCH: // %Q - Number of microseconds since 1970-01-01 00:00:00 UTC.
                {
                    int sec;
                    boolean negative = false;

                    if (text.charAt(pos) == '-') {
                        negative = true;
                        pos++;
                    }

                    /* TODO
                    set_hash("seconds",
                             rb_rational_new2(n,
                                              f_expt(INT2FIX(10),
                                                     INT2FIX(3))));
                     */

                    sec = getDigits(Integer.MAX_VALUE);
                    hash.put("_seconds", !negative ? sec : -sec);
                    hash.put("_seconds_rational", 1000);
                    break;
                }
                case FORMAT_SECONDS: // %S - Second of the minute (00..59)
                {
                    int sec = getDigits(2);
                    if (!validValueRange(sec, 0, 59)) {
                        throw newInvalidDataException();
                    } else {
                        hash.put("sec", sec);
                    }
                    break;
                }
                case FORMAT_EPOCH: // %s - Number of seconds since 1970-01-01 00:00:00 UTC.
                {
                    int sec;
                    boolean negative = false;

                    if (text.charAt(pos) == '-') {
                        negative = true;
                        pos++;
                    }
                    sec = getDigits(Integer.MAX_VALUE);
                    hash.put("seconds", !negative ? sec : -sec);
                    break;
                }
                case FORMAT_WEEK_YEAR_S: // %U, %OU - Week number of the year.  The week starts with Sunday.  (00..53)
                case FORMAT_WEEK_YEAR_M: // %W, %OW - Week number of the year.  The week starts with Monday.  (00..53)
                {
                    int w = getDigits(2);
                    if (!validValueRange(w, 0, 53)) {
                        throw newInvalidDataException();
                    } else {
                        if (token.getFormat() == Format.FORMAT_WEEK_YEAR_S) {
                            hash.put("wnum0", w);
                        } else {
                            hash.put("wnum1", w);
                        }
                    }
                    break;
                }
                case FORMAT_DAY_WEEK2: // %u, %Ou - Day of the week (Monday is 1, 1..7)
                {
                    int d = getDigits(1);
                    if (!validValueRange(d, 1, 7)) {
                        throw newInvalidDataException();
                    } else {
                        hash.put("cwday", d);
                    }
                    break;
                }
                case FORMAT_WEEK_WEEKYEAR: // %V, %OV - Week number of the week-based year (01..53)
                {
                    int w = getDigits(2);
                    if (!validValueRange(w, 1, 53)) {
                        throw newInvalidDataException();
                    } else {
                        hash.put("cweek", w);
                    }
                    break;
                }
                case FORMAT_DAY_WEEK: // %w - Day of the week (Sunday is 0, 0..6)
                {
                    int d = getDigits(1);
                    if (!validValueRange(d, 0, 6)) {
                        throw newInvalidDataException();
                    } else {
                        hash.put("wday", d);
                    }
                    break;
                }
                case FORMAT_YEAR_LONG:
                    // %Y, %EY - Year with century (can be negative, 4 digits at least)
                    //           -0001, 0000, 1995, 2009, 14292, etc.
                {
                    int y;
                    boolean negative = false;

                    if (isSign(text.charAt(pos))) {
                        negative = text.charAt(pos) == '-';
                        pos++;
                    }

                    if (matchAtNumPatterns(token)) {
                        y = getDigits(4);
                    } else {
                        y = getDigits(Integer.MAX_VALUE);
                    }

                    hash.put("year", !negative ? y : -y);
                    break;
                }
                case FORMAT_YEAR_SHORT: // %y, %Ey, %Oy - year % 100 (00..99)
                {
                    int y = getDigits(2);
                    if (!validValueRange(y, 0, 99)) {
                        throw newInvalidDataException();
                    }
                    hash.put("year", y);

                    if (hash.containsKey("_cent")) {
                        hash.put("_cent", y >= 69 ? 19 : 20);
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
                        hash.put("zone", zone);
                        pos += zone.length();

                        // offset
                        hash.put("offset", toDiff(zone));
                    } else {
                        throw newInvalidDataException();
                    }
                    break;
                }
                case FORMAT_SPECIAL:
                {
                    throw new Error("FORMAT_SPECIAL is a special token only for the lexer.");
                }
            }
        }

        return hash;
    }

    private int matchAtPatterns(String text, String[] patterns)
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

    private int getDigits(int len)
    {
        int v = 0;
        try {
            for (int i = 0; i < len; i++) {
                char c = text.charAt(pos + i); // IndexOutOfBounds
                pos += 1;
                if (!isDigit(c)) {
                    if (i > 0) {
                        break;
                    } else {
                        throw newInvalidDataException();
                    }
                } else {
                    v += v * 10 + toInt(c);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // ignorable error
        }
        return v;
    }
}