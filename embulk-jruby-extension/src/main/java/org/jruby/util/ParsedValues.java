package org.jruby.util;

import java.util.HashMap;
import java.util.Map;

import static org.jruby.util.RubyDateParser.isDigit;
import static org.jruby.util.RubyDateParser.isSign;
import static org.jruby.util.RubyDateParser.toInt;

public class ParsedValues
{
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

    public static int toDiff(String zone)
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

    int mday = -1;
    int wday = -1;
    int cwday = -1;
    int yday = -1;
    int cweek = -1;
    int cwyear = -1;
    int min = -1;
    int mon = -1;
    int hour = -1;
    int year = -1;
    int sec = -1;
    int wnum0 = -1;
    int wnum1 = -1;

    String zone = null;

    int sec_fraction = -1; // Rational
    int sec_fraction_rational = -1;

    int seconds = -1; // int or Rational
    int seconds_rational = -1;

    int _merid = -1;
    int _cent = -1;

    boolean fail = false;
    String leftover = null;

    public ParsedValues()
    {
    }

    void fail()
    {
        fail = true;
    }

    public HashMap<String, Object> toMap()
    {
        HashMap<String, Object> map = new HashMap<>();
        if (mday >= 0)
            map.put("mday", mday);

        if (wday >= 0)
            map.put("wday", wday);

        if (cwday >= 0)
            map.put("cwday", cwday);

        if (yday >= 0)
            map.put("yday", yday);

        if (cweek >= 0)
            map.put("cweek", cweek);

        if (cwyear >= 0)
            map.put("cwyear", cwyear);

        if (min >= 0)
            map.put("min", min);

        if (mon >= 0)
            map.put("mon", mon);

        if (hour >= 0)
            map.put("hour", hour);

        if (year >= 0)
            map.put("year", year);

        if (sec >= 0)
            map.put("sec", sec);

        if (wnum0 >= 0)
            map.put("wnum0", wnum0);

        if (wnum1 >= 0)
            map.put("wnum1", wnum1);

        if (zone != null) {
            map.put("zone", zone);
            map.put("offset", toDiff(zone));
        }

        if (sec_fraction >= 0) {
            // TODO return Rational
            map.put("sec_fraction", ((float)sec_fraction / sec_fraction_rational));
        }

        if (seconds >= 0) {
            if (seconds_rational >= 0) {
                map.put("seconds", ((float)seconds / seconds_rational));
            } else {
                map.put("seconds", seconds);
            }
        }

        if (_merid >= 0)
            map.put("_merid", _merid);

        if (_cent >= 0)
            map.put("_cent", _cent);

        return map;
    }
}
